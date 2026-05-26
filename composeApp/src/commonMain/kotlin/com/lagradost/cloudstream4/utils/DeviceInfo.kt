package com.lagradost.cloudstream4.utils

import kotlin.jvm.JvmInline

internal expect object DeviceInfo {
    fun getDeviceType(): DeviceType
    fun isLandscape(): Boolean
    fun getLayoutPreference(): Int
}

@JvmInline // This still works but has no affect on non-JVM targets
value class DeviceType(val value: Int) {
    infix fun or(other: DeviceType) = DeviceType(value or other.value)
}
