package com.lagradost.cloudstream3.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.webkit.*
import com.lagradost.api.Log
import com.lagradost.api.getContext
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.debugException
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.utils.Coroutines.atomicListOf
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.Coroutines.mainWork
import com.lagradost.cloudstream3.utils.Coroutines.runOnMainThread
import com.lagradost.nicehttp.HttpSendInterceptorContext
import com.lagradost.nicehttp.Interceptor
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.buildHeaders
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

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
        actual var webViewUserAgent: String? = null
        actual val DEFAULT_TIMEOUT = 60_000L
        private const val TAG = "WebViewResolver"

        @JvmName("getWebViewUserAgent1")
        fun getWebViewUserAgent(): String? {
            return webViewUserAgent ?: (getContext() as? Context)?.let { ctx ->
                runBlocking {
                    mainWork {
                        WebView(ctx).settings.userAgentString.also { userAgent ->
                            webViewUserAgent = userAgent
                        }
                    }
                }
            }
        }
    }

    actual override suspend fun intercept(ctx: HttpSendInterceptorContext): HttpClientCall {
        val request = ctx.request
        val fixedRequest = resolveUsingWebView(request).first
        return ctx.proceed(fixedRequest ?: request)
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

    @SuppressLint("SetJavaScriptEnabled")
    actual suspend fun resolveUsingWebView(
        request: HttpRequestBuilder,
        requestCallBack: (HttpRequestBuilder) -> Boolean,
    ): Pair<HttpRequestBuilder?, List<HttpRequestBuilder>> {
        val url = request.url.buildString()
        // Convert Ktor Headers to Map for WebView
        val headersMap = request.headers.build().entries()
            .associate { (key, values) -> key to values.last() }
        Log.i(TAG, "Initial web-view request: $url")
        var webView: WebView? = null
        var shouldExit = false

        fun destroyWebView() {
            main {
                webView?.stopLoading()
                webView?.destroy()
                webView = null
                shouldExit = true
                Log.i(TAG, "Destroyed webview")
            }
        }

        var fixedRequest: HttpRequestBuilder? = null
        val extraRequestList = atomicListOf<HttpRequestBuilder>()

        main {
            try {
                webView = WebView(
                    (getContext() as? Context)
                        ?: throw RuntimeException("No base context in WebViewResolver")
                ).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    webViewUserAgent = settings.userAgentString
                    if (userAgent != null) {
                        settings.userAgentString = userAgent
                    }
                }

                webView?.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = runBlocking {
                        val webViewUrl = request.url.toString()
                        Log.i(TAG, "Loading WebView URL: $webViewUrl")

                        if (script != null) {
                            runOnMainThread {
                                view.evaluateJavascript(script) { scriptCallback?.invoke(it) }
                            }
                        }

                        if (interceptUrl.containsMatchIn(webViewUrl)) {
                            fixedRequest = request.toRequest()?.also {
                                requestCallBack(it)
                            }
                            Log.i(TAG, "Web-view request finished: $webViewUrl")
                            destroyWebView()
                            return@runBlocking null
                        }

                        if (additionalUrls.any { it.containsMatchIn(webViewUrl) }) {
                            request.toRequest()?.also {
                                if (requestCallBack(it)) destroyWebView()
                            }?.let(extraRequestList::add)
                        }

                        val blacklistedFiles = listOf(
                            ".jpg", ".png", ".webp", ".mpg", ".mpeg", ".jpeg", ".webm",
                            ".mp4", ".mp3", ".gifv", ".flv", ".asf", ".mov", ".mng",
                            ".mkv", ".ogg", ".avi", ".wav", ".woff2", ".woff", ".ttf",
                            ".css", ".vtt", ".srt", ".ts", ".gif",
                            // Warning, this might fuck some future sites, but it's used to make Sflix work.
                            "wss://",
                        )

                        /** NOTE! request.requestHeaders is not perfect!
                         * They don't contain all the headers the browser actually gives.
                         * Overriding with okhttp might fuck up otherwise working requests,
                         * e.g the recaptcha request.
                         */
                        return@runBlocking try {
                            when {
                                blacklistedFiles.any {
                                    Url(webViewUrl).encodedPath.decodeURLPart().contains(it)
                                } || webViewUrl.endsWith("/favicon.ico") ->
                                    WebResourceResponse("image/png", null, null)

                                webViewUrl.contains("recaptcha") ||
                                    webViewUrl.contains("/cdn-cgi/") ->
                                    super.shouldInterceptRequest(view, request)

                                useOkhttp && request.method == "GET" ->
                                    (app.get(webViewUrl, headers = request.requestHeaders)
                                        as? NiceResponse)?.response?.toWebResourceResponse()

                                useOkhttp && request.method == "POST" ->
                                    (app.post(webViewUrl, headers = request.requestHeaders)
                                        as? NiceResponse)?.response?.toWebResourceResponse()

                                else -> super.shouldInterceptRequest(view, request)
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        handler?.proceed() // Ignore ssl issues
                    }
                }

                webView?.loadUrl(url, headersMap)
            } catch (e: Exception) {
                logError(e)
            }
        }

        var loop = 0
        val totalTime = timeout
        val delayTime = 100L

        while (loop < totalTime / delayTime && !shouldExit) {
            if (fixedRequest != null) return fixedRequest to extraRequestList
            delay(delayTime)
            loop += 1
        }

        Log.i(TAG, "Web-view timeout after ${totalTime / 1000}s")
        destroyWebView()
        return fixedRequest to extraRequestList
    }
}

fun WebResourceRequest.toRequest(): HttpRequestBuilder? {
    val webViewUrl = this.url.toString()
    return safe {
        HttpRequestBuilder().apply {
            method = HttpMethod(this@toRequest.method.uppercase())
            url(webViewUrl)
            this@toRequest.requestHeaders.forEach { (k, v) -> header(k, v) }
        }
    }
}

suspend fun HttpResponse.toWebResourceResponse(): WebResourceResponse {
    val contentTypeValue = headers["Content-Type"]
    // 1. contentType. 2. charset
    val typeRegex = Regex("""(.*);(?:.*charset=(.*)(?:|;)|)""")
    return if (contentTypeValue != null) {
        val found = typeRegex.find(contentTypeValue)
        val contentType = found?.groupValues?.getOrNull(1)?.ifBlank { null } ?: contentTypeValue
        val charset = found?.groupValues?.getOrNull(2)?.ifBlank { null }
        WebResourceResponse(contentType, charset, readRawBytes().inputStream())
    } else {
        WebResourceResponse("application/octet-stream", null, readRawBytes().inputStream())
    }
}
