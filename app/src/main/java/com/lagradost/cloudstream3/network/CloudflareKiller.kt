package com.lagradost.cloudstream3.network

import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.AnyThread
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.debugWarning
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.nicehttp.cookies
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.net.URI


//Provides async-able Calls
class ContinuationCallback(
    private val call: Call,
    private val continuation: CancellableContinuation<Response>
) : Callback, CompletionHandler {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onResponse(call: Call, response: Response) {
        continuation.resume(response, null)
    }

    override fun onFailure(call: Call, e: java.io.IOException) {
        // Cannot throw exception on SocketException since that can lead to un-catchable crashes
        // when you exit an activity as a request
        println("Exception in NiceHttp: ${e.javaClass.name} ${e.message}")
        if (call.isCanceled()) {
            // Must be able to throw errors, for example timeouts
            if (e is java.io.InterruptedIOException)
                continuation.cancel(e)
            else
                e.printStackTrace()
        } else {
            continuation.resumeWithException(e)
        }
    }

    override fun invoke(cause: Throwable?) {
        try {
            call.cancel()
        } catch (_: Throwable) {
        }
    }
}


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

        suspend inline fun Call.await(): Response {
            return suspendCancellableCoroutine { continuation ->
                val callback = ContinuationCallback(this, continuation)
                enqueue(callback)
                continuation.invokeOnCancellation(callback)
            }
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
     * */
    fun getCookieHeaders(url: String): Headers {
        val userAgentHeaders = WebViewResolver.webViewUserAgent?.let {
            mapOf("user-agent" to it)
        } ?: emptyMap()

        return getHeaders(userAgentHeaders, savedCookies[URI(url).host] ?: emptyMap())
    }

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()

        when (val cookies = savedCookies[request.url.host]) {
            null -> {
                val response = chain.proceed(request)
                if(!(response.header("Server") in CLOUDFLARE_SERVERS && response.code in ERROR_CODES)) {
                    return@runBlocking response
                } else {
                    response.close()
                    bypassCloudflare(request)?.let {
                        Log.d(TAG, "Succeeded bypassing cloudflare: ${request.url}")
                        return@runBlocking it
                    }
                }
            }
            else -> {
                return@runBlocking proceed(request, cookies)
            }
        }

        debugWarning({ true }) { "Failed cloudflare at: ${request.url}" }
        return@runBlocking chain.proceed(request)
    }

    private fun getWebViewCookie(url: String): String? {
        return safe {
            CookieManager.getInstance()?.getCookie(url)
        }
    }

    /**
     * Returns true if the cf cookies were successfully fetched from the CookieManager
     * Also saves the cookies.
     * */
    private fun trySolveWithSavedCookies(request: Request): Boolean {
        // Not sure if this takes expiration into account
        return getWebViewCookie(request.url.toString())?.let { cookie ->
            cookie.contains("cf_clearance").also { solved ->
                if (solved) savedCookies[request.url.host] = parseCookieMap(cookie)
            }
        } ?: false
    }

    private suspend fun proceed(request: Request, cookies: Map<String, String>): Response {
        val userAgentMap = WebViewResolver.getWebViewUserAgent()?.let {
            mapOf("user-agent" to it)
        } ?: emptyMap()

        val headers =
            getHeaders(request.headers.toMap() + userAgentMap, cookies + request.cookies)
        val okHttpClient = (app.baseClient.engine as? io.ktor.client.engine.okhttp.OkHttpEngine)
            ?.config?.preconfigured ?: okhttp3.OkHttpClient()
        return okHttpClient.newCall(
            request.newBuilder()
                .headers(headers)
                .build()
        ).await()
    }

    private suspend fun bypassCloudflare(request: Request): Response? {
        val url = request.url.toString()

        // If no cookies then try to get them
        // Remove this if statement if cookies expire
        if (!trySolveWithSavedCookies(request)) {
            Log.d(TAG, "Loading webview to solve cloudflare for ${request.url}")
            WebViewResolver(
                // Never exit based on url
                Regex(".^"),
                // Cloudflare needs default user agent
                userAgent = null,
                // Cannot use okhttp (i think intercepting cookies fails which causes the issues)
                useOkhttp = false,
                // Match every url for the requestCallBack
                additionalUrls = listOf(Regex("."))
            ).resolveUsingWebView(
                url
            ) {
                trySolveWithSavedCookies(request)
            }
        }

        val cookies = savedCookies[request.url.host] ?: return null
        return proceed(request, cookies)
    }
}
