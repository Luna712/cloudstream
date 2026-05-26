package com.lagradost.cloudstream4.utils

internal expect object DeviceInfo {
    fun getDeviceLayout(): DeviceLayout.Layout
    fun isLandscape(): Boolean
    fun getLayoutPreference(): Int
}
