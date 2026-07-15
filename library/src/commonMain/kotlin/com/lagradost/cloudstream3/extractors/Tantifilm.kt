package com.lagradost.cloudstream3.extractors

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class Tantifilm : ExtractorApi() {
    override val name = "Tantifilm"
    override val mainUrl = "https://cercafilm.net"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val link = "$mainUrl/api/source/${url.substringAfterLast("/")}"
        val response = app.post(link).text().replace("""\""","")
        val jsonVideoData = parseJson<TantifilmJsonData>(response)
        return jsonVideoData.data.map {
            newExtractorLink(
                this.name,
                this.name,
                it.file + ".${it.type}",
            ) {
                this.referer = mainUrl
                this.quality = it.label.filter{ it.isDigit() }.toInt()
            }
        }
    }

    @Serializable
    data class TantifilmJsonData(
        @SerialName("success") val success: Boolean,
        @SerialName("data") val data: List<TantifilmData>,
        @SerialName("captions") val captions: List<String>,
        @SerialName("is_vr") val isVr: Boolean,
    )

    @Serializable
    data class TantifilmData(
        @SerialName("file") val file: String,
        @SerialName("label") val label: String,
        @SerialName("type") val type: String,
    )
}
