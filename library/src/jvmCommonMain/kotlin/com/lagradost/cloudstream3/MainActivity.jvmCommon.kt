package com.lagradost.cloudstream3

import io.ktor.client.engine.okhttp.OkHttpEngine
import okhttp3.OkHttpClient

// TODO: Remove usage of this by migrating interceptors and media3 to ktor
@InternalAPI
val okHttpClient = (app.baseClient.engine as? OkHttpEngine)
    ?.config?.preconfigured ?: OkHttpClient()
