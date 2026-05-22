package com.lagradost.cloudstream3.shared

internal expect object DeviceInfo {
    fun isTVDevice(): Boolean
    fun getLayoutPreference(): Int
}
