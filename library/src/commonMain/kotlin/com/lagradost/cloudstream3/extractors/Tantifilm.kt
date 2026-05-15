package com.lagradost.cloudstream3.extractors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Tantifilm : ExtractorApi() {
    override var name = "Tantifilm"
    override var mainUrl = "https://cercafilm.net"
    override val requiresReferer = false

    @Serializable
    data class TantifilmJsonData (
        @SerialName("success") val success : Boolean,
        @SerialName("data") val data : List<TantifilmData>,
        @SerialName("captions")val captions : List<String>,
        @SerialName("is_vr") val is_vr : Boolean
    )

    @Serializable
    data class TantifilmData (
        @SerialName("file") val file : String,
        @SerialName("label") val label : String,
        @SerialName("type") val type : String
    )

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val link = "$mainUrl/api/source/${url.substringAfterLast("/")}"
        val response = app.post(link).text().replace("""\""","")
        val jsonvideodata = parseJson<TantifilmJsonData>(response)
        return jsonvideodata.data.map {
            newExtractorLink(
                this.name,
                this.name,
                it.file+".${it.type}"
            ) {
                this.referer = mainUrl
                this.quality = it.label.filter{ it.isDigit() }.toInt()
            }
        }
    }
}