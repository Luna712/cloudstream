package com.lagradost.cloudstream3.extractors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink

open class GUpload: ExtractorApi() {
    override val name: String = "GUpload"
    override val mainUrl: String = "https://gupload.xyz"
    override val requiresReferer: Boolean = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url, referer = referer).text()

        val playerConfigEncoded = response.substringAfter("decodePayload('").substringBefore("');")
        val playerConfigString = base64Decode(playerConfigEncoded).substringAfter("|")

        val playerConfig = AppUtils.parseJson<VideoInfo>(playerConfigString)

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = playerConfig.videoUrl.replace("\\", ""),
            ) {
                Regex("/(\\d+p)\\.").find(playerConfig.videoUrl)?.groupValues?.get(1)?.let {
                    quality = getQualityFromName(it)
                }
            }
        )
    }

    @Serializable
    private data class VideoInfo(
        @SerialName("videoUrl") val videoUrl: String,
        @SerialName("posterUrl") val posterUrl: String? = null,
        @SerialName("videoId") val videoId: String? = null,
        @SerialName("primaryColor") val primaryColor: String? = null,
        @SerialName("audioTracks") val audioTracks: List<JsonElement> = emptyList(),
        @SerialName("subtitleTracks") val subtitleTracks: List<JsonElement> = emptyList(),
        @SerialName("vastFallbackList") val vastFallbackList: List<String> = emptyList(),
        @SerialName("videoOwnerId") val videoOwnerId: Long = 0,
    )
}
