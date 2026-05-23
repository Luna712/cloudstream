package com.lagradost.cloudstream3

internal actual object DeviceInfo {
    actual fun isTVDevice(): Boolean
    actual fun isLandscape(): Boolean
    actual fun getLayoutPreference(): Int
}
