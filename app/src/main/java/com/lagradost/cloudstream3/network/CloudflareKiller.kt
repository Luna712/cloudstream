package com.lagradost.cloudstream3.network

import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.AnyThread
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.debugWarning
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.nicehttp.kmp.HttpSendInterceptorContext
import com.lagradost.nicehttp.kmp.Interceptor
import com.lagradost.nicehttp.kmp.buildHeaders
import com.lagradost.nicehttp.kmp.getRequestCookies
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.URI

@AnyThread
class CloudflareKiller : Interceptor {
    companion object {
        const val TAG = "CloudflareKiller"
        private val ERROR_CODES = listOf(403, 503)
        private val CLOUDFLARE_SERVERS = listOf("cloudflare-nginx", "cloudflare")

        fun parseCookieMap(cookie: String): Map<String, String> {
            return cookie.split(";").associate {
                val split = it.split("=")
                (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
            }.filter { it.key.isNotBlank() && it.value.isNotBlank() }
        }
    }

    init {
        // Needs to clear cookies between sessions to generate new cookies.
        safe {
            // This can throw an exception on unsupported devices :(
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    val savedCookies: MutableMap<String, Map<String, String>> = mutableMapOf()

    /**
     * Gets the headers with cookies, webview user agent included!
     */
    fun getCookieHeaders(url: String): Headers {
        val userAgentHeaders = WebViewResolver.webViewUserAgent?.let {
            mapOf("user-agent" to it)
        } ?: emptyMap()
        return buildHeaders(
            userAgentHeaders,
            referer = null,
            cookie = savedCookies[URI(url).host] ?: emptyMap()
        )
    }

    override suspend fun intercept(ctx: HttpSendInterceptorContext): HttpClientCall {
        val request = ctx.request
        val host = request.url.host

        val cookies = savedCookies[host]
        if (cookies != null) {
            return proceed(request, cookies, ctx)
        }

        val call = ctx.proceed()
        val serverHeader = call.response.headers["Server"]
        val code = call.response.status.value

        if (!(serverHeader in CLOUDFLARE_SERVERS && code in ERROR_CODES)) {
            return call
        }

        // Cloudflare detected — try to bypass
        bypassCloudflare(request, ctx)?.let {
            Log.d(TAG, "Succeeded bypassing cloudflare: ${request.url.buildString()}")
            return it
        }

        debugWarning({ true }) { "Failed cloudflare at: ${request.url.buildString()}" }
        return ctx.proceed()
    }

    private fun getWebViewCookie(url: String): String? {
        return safe {
            CookieManager.getInstance()?.getCookie(url)
        }
    }

    /**
     * Returns true if the cf cookies were successfully fetched from the CookieManager.
     * Also saves the cookies.
     */
    private fun trySolveWithSavedCookies(url: String, host: String): Boolean {
        return getWebViewCookie(url)?.let { cookie ->
            cookie.contains("cf_clearance").also { solved ->
                if (solved) savedCookies[host] = parseCookieMap(cookie)
            }
        } ?: false
    }

    private suspend fun proceed(
        request: HttpRequestBuilder,
        cookies: Map<String, String>,
        ctx: HttpSendInterceptorContext,
    ): HttpClientCall {
        val userAgentMap = WebViewResolver.getWebViewUserAgent()?.let {
            mapOf("user-agent" to it)
        } ?: emptyMap()

        val existingCookies = request.headers.build().getRequestCookies()
        val mergedCookies = existingCookies + cookies

        return ctx.proceed {
            userAgentMap.forEach { (k, v) ->
                headers.remove(k)
                headers.append(k, v)
            }
            headers.remove("Cookie")
            if (mergedCookies.isNotEmpty()) {
                header(
                    "Cookie",
                    mergedCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                )
            }
        }
    }

    private suspend fun bypassCloudflare(
        request: HttpRequestBuilder,
        ctx: HttpSendInterceptorContext,
    ): HttpClientCall? {
        val url = request.url.buildString()
        val host = request.url.host

        if (!trySolveWithSavedCookies(url, host)) {
            Log.d(TAG, "Loading webview to solve cloudflare for $url")
            WebViewResolver(
                // Never exit based on url
                Regex(".^"),
                // Cloudflare needs default user agent
                userAgent = null,
                // Cannot use okhttp (intercepting cookies fails which causes the issues)
                useOkhttp = false,
                // Match every url for the requestCallBack
                additionalUrls = listOf(Regex("."))
            ).resolveUsingWebView(url) {
                trySolveWithSavedCookies(url, host)
            }
        }

        val cookies = savedCookies[host] ?: return null
        return proceed(request, cookies, ctx)
    }
}
