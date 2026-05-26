package com.lagradost.cloudstream4.utils

// DeviceType is intentionally hidden from IDE autocomplete and external callers.
// Use DeviceLayout.isLayout() with DeviceLayout.PHONE, DeviceLayout.TV etc. instead.
@Deprecated(
    message = "Use DeviceLayout.isLayout() instead",
    level = DeprecationLevel.HIDDEN,
)
internal enum class DeviceType {
    PHONE, TV, EMULATOR, COMPUTER
}

internal expect object DeviceInfo {
    fun getDeviceType(): DeviceType
    fun isLandscape(): Boolean
    fun getLayoutPreference(): Int
}
