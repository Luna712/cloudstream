package com.lagradost.cloudstream3.network

import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.HttpSendInterceptorContext
import com.lagradost.nicehttp.Interceptor
import io.ktor.client.call.*
import io.ktor.client.request.*

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
expect class WebViewResolver(
    interceptUrl: Regex,
    additionalUrls: List<Regex> = emptyList(),
    userAgent: String? = USER_AGENT,
    useOkhttp: Boolean = true,
    script: String? = null,
    scriptCallback: ((String) -> Unit)? = null,
    timeout: Long = DEFAULT_TIMEOUT,
) : Interceptor {
    companion object {
        val DEFAULT_TIMEOUT: Long
        var webViewUserAgent: String?
    }

    override suspend fun intercept(ctx: HttpSendInterceptorContext): HttpClientCall

    /**
     * @param requestCallBack asynchronously return matched requests by either interceptUrl or additionalUrls. If true, destroy WebView.
     * @return the final request (by interceptUrl) and all the collected urls (by additionalUrls).
     */
    suspend fun resolveUsingWebView(
        url: String,
        referer: String? = null,
        method: String = "GET",
        requestCallBack: (HttpRequestBuilder) -> Boolean = { false },
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>>

    /**
     * @param requestCallBack asynchronously return matched requests by either interceptUrl or additionalUrls. If true, destroy WebView.
     * @return the final request (by interceptUrl) and all the collected urls (by additionalUrls).
     */
    suspend fun resolveUsingWebView(
        url: String,
        referer: String? = null,
        headers: Map<String, String> = emptyMap(),
        method: String = "GET",
        requestCallBack: (HttpRequestBuilder) -> Boolean = { false },
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>>

    /**
     * @param requestCallBack asynchronously return matched requests by either interceptUrl or additionalUrls. If true, destroy WebView.
     * @return the final request (by interceptUrl) and all the collected urls (by additionalUrls).
     */
    suspend fun resolveUsingWebView(
        request: HttpRequestBuilder,
        requestCallBack: (HttpRequestBuilder) -> Boolean = { false },
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>>
}
