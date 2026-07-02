package com.lagradost.cloudstream3.network

import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.HttpSendInterceptorContext
import com.lagradost.nicehttp.Interceptor
import io.ktor.client.call.*
import io.ktor.client.request.*

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

    override suspend fun intercept(ctx: HttpSendInterceptorContext): HttpClientCall {
        // No WebView on JS/WASM, just proceed with the request as-is
        return ctx.proceed()
    }

    actual suspend fun resolveUsingWebView(
        url: String,
        referer: String?,
        method: String,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> {
        throw UnsupportedOperationException("WebViewResolver is not supported on JS/WASM targets.")
    }

    actual suspend fun resolveUsingWebView(
        url: String,
        referer: String?,
        headers: Map<String, String>,
        method: String,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> {
        throw UnsupportedOperationException("WebViewResolver is not supported on JS/WASM targets.")
    }

    actual suspend fun resolveUsingWebView(
        request: HttpRequestBuilder,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> {
        throw UnsupportedOperationException("WebViewResolver is not supported on JS/WASM targets.")
    }
}
