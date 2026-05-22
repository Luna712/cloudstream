package com.lagradost.cloudstream3

internal expect object DeviceInfo {
    fun isTVDevice(): Boolean
    fun getLayoutPreference(): Int
}
