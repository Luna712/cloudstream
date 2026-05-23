package com.lagradost.cloudstream3

internal actual object DeviceInfo {
    actual fun isTVDevice(): Boolean = false
    actual fun isLandscape(): Boolean = false
    actual fun getLayoutPreference(): Int = -1
}
