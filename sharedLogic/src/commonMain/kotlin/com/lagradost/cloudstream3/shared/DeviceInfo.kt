package com.lagradost.cloudstream3.shared

internal expect object DeviceInfo {
    fun isUIModeTV(): Boolean
    fun getModel(): String
    fun getLayoutPreference(): Int
}
