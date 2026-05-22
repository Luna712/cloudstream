package com.lagradost.cloudstream3.utils

import android.content.Context
import com.lagradost.cloudstream3.shared.BuildConfig


/**
 * Simple helper to get the short commit hash from assets.
 * The hash is generated at build and stored as an asset
 * that can be accessed at runtime for Gradle
 * configuration cache support.
 */
object GitInfo {
    fun Context.currentCommitHash(): String = try {
        BuildConfig.GIT_HASH
    } catch (_: Exception) {
        ""
    }
}
