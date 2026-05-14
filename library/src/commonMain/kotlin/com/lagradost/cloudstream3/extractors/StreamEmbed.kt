package com.lagradost.cloudstream3.extractors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper

open class StreamEmbed : ExtractorApi() {
    override var name = "StreamEmbed"
    override var mainUrl = "https://watch.gxplayer.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val jsonString = app.get(url, referer = mainUrl).text()
            .substringAfter("var video = ").substringBefore(";")
        val video = parseJson<Details>(jsonString)

        M3u8Helper.generateM3u8(
            this.name,
            "$mainUrl/m3u8/${video.uid}/${video.md5}/master.txt?s=1&id=${video.id}&cache=${video.status}",
            referer = "$mainUrl/",
        ).forEach(callback)
    }

    @Serializable
    private data class Details(
        @SerialName("id") val id: String,
        @SerialName("uid") val uid: String,
        @SerialName("slug") val slug: String,
        @SerialName("title") val title: String,
        @SerialName("quality") val quality: String,
        @SerialName("type") val type: String,
        @SerialName("status") val status: String,
        @SerialName("md5") val md5: String,
    )
}
