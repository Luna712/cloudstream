package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAudioFile
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink

class YoutubeShortLinkExtractor : YoutubeExtractor() {
    override val mainUrl = "https://youtu.be"
}

class YoutubeMobileExtractor : YoutubeExtractor() {
    override val mainUrl = "https://m.youtube.com"
}

class YoutubeNoCookieExtractor : YoutubeExtractor() {
    override val mainUrl = "https://www.youtube-nocookie.com"
}

private data class PlayerResponse(
    val streamingData: StreamingData? = null,
    val videoDetails: VideoDetails? = null,
    val captions: CaptionsHolder? = null,
)

private data class StreamingData(
    val hlsManifestUrl: String? = null,
    val adaptiveFormats: List<Format> = emptyList(),
)

private data class Format(
    val itag: Int = 0,
    val url: String? = null,
    val mimeType: String? = null,   // e.g. "video/webm; codecs=\"vp9\""
    val width: Int? = null,
    val height: Int? = null,
    val audioSampleRate: String? = null,
    val audioChannels: Int? = null,
)

private data class VideoDetails(
    val isLive: Boolean = false,
    val isLiveContent: Boolean = false,
)

private data class CaptionsHolder(
    val playerCaptionsTracklistRenderer: CaptionTracklistRenderer? = null,
)

private data class CaptionTracklistRenderer(
    val captionTracks: List<CaptionTrack> = emptyList(),
)

private data class CaptionTrack(
    val baseUrl: String? = null,
    val name: RunsHolder? = null,
    val languageCode: String? = null,
)

private data class RunsHolder(
    val runs: List<Run> = emptyList(),
)

private data class Run(val text: String? = null)

open class YoutubeExtractor : ExtractorApi() {

    override val mainUrl = "https://www.youtube.com"
    override val name = "YouTube"
    override val requiresReferer = false

    // Innertube constants (web client)
    private val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val INNERTUBE_CLIENT = mapOf(
        "clientName" to "WEB",
        "clientVersion" to "2.20240101.00.00",
    )

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val videoId = extractYouTubeId(url)
        val response = fetchPlayerResponse(videoId)

        val isLive = response.videoDetails?.isLive == true
            || response.videoDetails?.isLiveContent == true

        val hlsUrl = response.streamingData?.hlsManifestUrl

        if (isLive && hlsUrl != null) {
            callback(
                newExtractorLink(
                    source = name,
                    name   = "YouTube Live",
                    url    = hlsUrl,
                ) {
                    type = ExtractorLinkType.M3U8
                }
            )
            return
        }

        processVideo(response, subtitleCallback, callback)
    }

    private suspend fun fetchPlayerResponse(videoId: String): PlayerResponse {
        val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY"

        val body = mapOf(
            "videoId" to videoId,
            "context" to mapOf("client" to INNERTUBE_CLIENT),
            "params" to "2AMBCgIQBg==", // tells Innertube to include adaptive formats
        )

        return app.post(
            url = endpoint,
            json = body,
            headers = mapOf(
                "Content-Type" to "application/json",
                "User-Agent" to "Mozilla/5.0",
                "Origin" to "https://www.youtube.com",
                "Referer" to "https://www.youtube.com/watch?v=$videoId",
            ),
        ).parsedSafe<PlayerResponse>()
            ?: throw ErrorLoadingException("Failed parsing player response")
    }

    private suspend fun processVideo(
        response: PlayerResponse,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val formats = response.streamingData?.adaptiveFormats.orEmpty()

        val videoFormats = formats.filter { it.width != null && it.height != null && it.url != null }
        val audioFormats = formats.filter { it.audioSampleRate != null && it.url != null }

        if (videoFormats.isEmpty()) return false

        val audioTracks = audioFormats.map { newAudioFile(it.url!!) }

        videoFormats.forEach { video ->
            val codec = codecFromMimeType(video.mimeType)
            val quality = video.height ?: 0

            callback(
                newExtractorLink(
                    source = name,
                    name = "YouTube ${normalizeCodec(codec)}",
                    url = video.url!!,
                ) {
                    this.quality = quality
                    this.audioTracks = audioTracks
                }
            )
        }

        // Subtitles
        response.captions
            ?.playerCaptionsTracklistRenderer
            ?.captionTracks
            ?.forEach { track ->
                val captionUrl = track.baseUrl ?: return@forEach
                val lang = track.name?.runs?.firstOrNull()?.text
                    ?: track.languageCode
                    ?: "Unknown"

                subtitleCallback(newSubtitleFile(lang = lang, url = captionUrl))
            }

        return true
    }

    // ---------------- HELPERS ----------------

    private fun extractYouTubeId(url: String): String {
        val regex = Regex(
            "(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:.*v=|v/|u/\\w/|embed/|shorts/|live/))([\\w-]{11})"
        )
        return regex.find(url)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Invalid YouTube URL: $url")
    }

    /** Pulls the codec string out of a mimeType like `video/webm; codecs="vp9"` */
    private fun codecFromMimeType(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        val match = Regex("""codecs="([^"]+)"""").find(mimeType) ?: return null
        return match.groupValues[1].split(',').firstOrNull()?.trim()
    }

    private fun normalizeCodec(codec: String?): String {
        if (codec.isNullOrBlank()) return ""
        val c = codec.lowercase()
        return when {
            c.startsWith("av01") -> "AV1"
            c.startsWith("vp9") -> "VP9"
            c.startsWith("avc1") || c.startsWith("h264") -> "H264"
            c.startsWith("hev1") || c.startsWith("hvc1") || c.startsWith("hevc") -> "H265"
            else -> codec.substringBefore('.').uppercase()
        }
    }
}
