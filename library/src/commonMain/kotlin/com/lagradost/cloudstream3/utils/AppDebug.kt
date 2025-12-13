package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.Prerelease

@Prerelease
object AppDebug {
    @Volatile
    var isDebug: Boolean = false
}
