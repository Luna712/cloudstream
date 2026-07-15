// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.lagradost.cloudstream3.extractors

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class TauVideo : ExtractorApi() {
    override val name = "TauVideo"
    override val mainUrl = "https://tau-video.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val extRef = referer ?: ""
        val videoKey = url.split("/").last()
        val videoUrl = "$mainUrl/api/video/$videoKey"
        val api = app.get(videoUrl).parsedSafe<TauVideoUrls>() ?: throw ErrorLoadingException("TauVideo")

        for (video in api.urls) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = video.url,
                ) {
                    this.referer = extRef
                    this.quality = getQualityFromName(video.label)
                }
            )
        }
    }

    @Serializable
    data class TauVideoUrls(
        @SerialName("urls") val urls: List<TauVideoData>,
    )

    @Serializable
    data class TauVideoData(
        @SerialName("url") val url: String,
        @SerialName("label") val label: String,
    )
}
