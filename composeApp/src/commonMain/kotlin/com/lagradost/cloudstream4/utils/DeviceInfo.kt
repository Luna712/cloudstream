package com.lagradost.cloudstream4.utils

internal expect object DeviceInfo {
    private enum class DeviceType {
        PHONE, TV, EMULATOR, COMPUTER
    }

    fun getDeviceType(): DeviceType
    fun isLandscape(): Boolean
    fun getLayoutPreference(): Int
}
