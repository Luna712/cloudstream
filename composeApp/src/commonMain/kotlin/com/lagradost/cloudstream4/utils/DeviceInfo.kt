package com.lagradost.cloudstream4.utils

internal expect object DeviceInfo {
    fun getDeviceType(): DeviceType
    fun isLandscape(): Boolean
    fun getLayoutPreference(): Int
}

// HIDDEN prevents this from showing up in auto completion at all.
@Deprecated(
    message = "Use DeviceLayout.isLayout() instead",
    level = DeprecationLevel.HIDDEN,
)
internal enum class DeviceType {
    PHONE, TV, EMULATOR, COMPUTER
}
