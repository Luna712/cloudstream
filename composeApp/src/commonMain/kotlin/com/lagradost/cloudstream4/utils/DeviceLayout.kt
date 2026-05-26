package com.lagradost.cloudstream4.utils

import com.lagradost.cloudstream4.preferences.PreferenceDefaults

object DeviceLayout {
    value class Layout(val value: Int) {
        companion object {
            val PHONE = Layout(0b00001)
            val TV = Layout(0b00010)
            val EMULATOR = Layout(0b00100)
            val COMPUTER = Layout(0b01000)
        }

        infix fun or(other: Layout) = Layout(value or other.value)
    }

    private var layoutId = -1
    // TODO when fully on Compose
    // private val layoutId: Int get() = resolveLayout()

    /**
     * Returns true if the layout is any of the flags, so
     * so isLayout(TV or EMULATOR) is a valid statement 
     * for checking if the layout is in the emulator
     * or tv. Auto will become the "TV" or the
     * "PHONE" layout.
     *
     * Valid flags are: PHONE, TV, EMULATOR, or COMPUTER
     */
    fun isLayout(flags: Layout): Boolean = (layoutId and flags.value) != 0

    /** Returns true if the current orientation is landscape. */
    fun isLandscape(): Boolean =
        isLayout(TV or EMULATOR) || DeviceInfo.isLandscape()

    /**
     * Updates the cached layout ID from preferences and device detection.
     *
     * TODO: Remove caching once fully migrated to Compose, layout will be read
     * directly via [DeviceInfo] during composition where caching is handled by
     * the Compose runtime.
     */
    fun update() {
        layoutId = resolveLayout()
    }

    private fun resolveLayout(): Int {
        return when (DeviceInfo.getLayoutPreference()) {
            PreferenceDefaults.APP_LAYOUT -> DeviceInfo.getDeviceLayout()
            0 -> PHONE
            1 -> TV
            2 -> EMULATOR
            else -> PHONE
        }
    }
}
