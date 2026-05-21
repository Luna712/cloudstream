package com.lagradost.cloudstream3.shared

internal expect object DeviceInfo {
    val UI_MODE_TELEVISION: Int
    fun getModel(): String
    fun getUIMode(): Int
    fun getLayoutPreference(): Int
}
