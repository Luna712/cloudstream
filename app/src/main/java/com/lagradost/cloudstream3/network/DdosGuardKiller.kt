package com.lagradost.cloudstream3.network

import androidx.annotation.AnyThread
import com.lagradost.cloudstream3.app
import com.lagradost.nicehttp.HttpSendInterceptorContext
import com.lagradost.nicehttp.Interceptor
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.getRequestCookies
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * @param alwaysBypass will pre-emptively fetch ddos guard cookies if true.
 * If false it will only try to get cookies when a request returns 403
 */
// As seen in https://github.com/anime-dl/anime-downloader/blob/master/anime_downloader/sites/erairaws.py
@AnyThread
class DdosGuardKiller(private val alwaysBypass: Boolean) : Interceptor {
    val savedCookiesMap = mutableMapOf<String, Map<String, String>>()

    private var ddosBypassPath: String? = null

    override suspend fun intercept(ctx: HttpSendInterceptorContext): HttpClientCall {
        val request = ctx.request
        if (alwaysBypass) return bypassDdosGuard(request, ctx)

        val call = ctx.proceed()
        return if (call.response.status.value == 403) {
            bypassDdosGuard(request, ctx)
        } else call
    }

    private suspend fun bypassDdosGuard(
        request: HttpRequestBuilder,
        ctx: HttpSendInterceptorContext,
    ): HttpClientCall {
        ddosBypassPath = ddosBypassPath ?: Regex("'(.*?)'").find(
            app.get("https://check.ddos-guard.net/check.js").text()
        )?.groupValues?.get(1)

        val host = request.url.host
        val scheme = request.url.protocol.name

        val cookies = savedCookiesMap[host]
            // If no cookies are found fetch and save em.
            ?: "$scheme://$host${ddosBypassPath ?: ""}".let {
                // Somehow app.get fails
                Requests().get(it).cookies.also { cookies ->
                    savedCookiesMap[host] = cookies
                }
            }

        // Use getRequestCookies() from commonMain to extract existing cookies
        val existingCookies = request.headers.build().getRequestCookies()
        val mergedCookies = existingCookies + cookies

        return ctx.proceed {
            headers.remove("Cookie")
            if (mergedCookies.isNotEmpty()) {
                header(
                    "Cookie",
                    mergedCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                )
            }
        }
    }
}
