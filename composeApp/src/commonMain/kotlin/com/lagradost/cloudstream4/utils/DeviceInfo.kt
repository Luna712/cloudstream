package com.lagradost.cloudstream4.utils

internal expect object DeviceInfo {
    fun getDeviceLayout(): Int
    fun isLandscape(): Boolean
    fun getLayoutPreference(): Int
}
