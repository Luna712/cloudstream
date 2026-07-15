package com.lagradost.cloudstream3.network

import com.lagradost.cloudstream3.mvvm.debugException
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.nicehttp.HttpSendInterceptorContext
import com.lagradost.nicehttp.Interceptor
import com.lagradost.nicehttp.buildHeaders
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * When used as Interceptor additionalUrls cannot be returned, use WebViewResolver(...).resolveUsingWebView(...)
 * @param interceptUrl will stop the WebView when reaching this url.
 * @param additionalUrls this will make resolveUsingWebView also return all other requests matching the list of Regex.
 * @param userAgent if null then will use the default user agent
 * @param useOkhttp will try to use the okhttp client as much as possible, but this might cause some requests to fail. Disable for cloudflare.
 * @param script pass custom js to execute
 * @param scriptCallback will be called with the result from custom js
 * @param timeout close webview after timeout
 */
actual class WebViewResolver actual constructor(
    val interceptUrl: Regex,
    val additionalUrls: List<Regex>,
    val userAgent: String?,
    val useOkhttp: Boolean,
    val script: String?,
    val scriptCallback: ((String) -> Unit)?,
    val timeout: Long,
) : Interceptor {

    actual companion object {
        actual val DEFAULT_TIMEOUT = 60_000L
        actual var webViewUserAgent: String? = null
    }

    actual override suspend fun intercept(ctx: HttpSendInterceptorContext): HttpClientCall {
        return ctx.proceed()
    }

    actual suspend fun resolveUsingWebView(
        url: String,
        referer: String?,
        method: String,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> =
        resolveUsingWebView(url, referer, emptyMap(), method, requestCallBack)

    actual suspend fun resolveUsingWebView(
        url: String,
        referer: String?,
        headers: Map<String, String>,
        method: String,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> {
        return try {
            resolveUsingWebView(
                HttpRequestBuilder().apply {
                    this.method = HttpMethod(method.uppercase())
                    url(url)
                    buildHeaders(headers, referer, emptyMap()).forEach { k, values ->
                        values.forEach { v -> header(k, v) }
                    }
                },
                requestCallBack,
            )
        } catch (e: IllegalArgumentException) {
            logError(e)
            debugException { "ILLEGAL URL IN resolveUsingWebView!" }
            null to emptyList()
        }
    }

    actual suspend fun resolveUsingWebView(
        request: HttpRequestBuilder,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> {
        throw UnsupportedOperationException("WebViewResolver is not supported on this platform.")
    }
}
