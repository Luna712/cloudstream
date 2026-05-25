package com.lagradost.cloudstream3.utils

import io.ktor.http.Url
import io.ktor.http.URLBuilder

object StringUtils {
    fun String.encodeUrl(): String {
        return try {
            URLBuilder(Url(this)).buildString()
        } catch (e: Exception) {
            // Fallback for malformed URLs
            this 
        }
    }

    fun String.decodeUrl(): String {
        return try {
            val parsed = Url(this)
            URLBuilder().apply {
                protocol = parsed.protocol
                host = parsed.host
                port = parsed.port
                path(parsed.pathSegments)
                parameters.appendAll(parsed.parameters)
                fragment = parsed.fragment
            }.buildString()
        } catch (e: Exception) {
            this
        }
    }

    // Deprecate after next stable

    /* @Deprecated(
        message = "Use Ktor 'Url' naming convention instead.",
        replaceWith = ReplaceWith("this.encodeUrl()")
    ) */
    fun String.decodeUri(): String = encodeUrl()

    /* @Deprecated(
        message = "Use Ktor 'Url' naming convention instead.",
        replaceWith = ReplaceWith("this.decodeUrl()")
    ) */
    fun String.decodeUri(): String = decodeUrl()
}

