package com.lagradost.cloudstream3

object DeviceLayout {
    const val PHONE: Int = 0b001
    const val TV: Int = 0b010
    const val EMULATOR: Int = 0b100

    private var layoutId = -1

    fun isLayout(flags: Int): Boolean = (layoutId and flags) != 0

    fun isLandscape(): Boolean =
        isLayout(TV or EMULATOR) || DeviceInfo.isLandscape()

    fun update() {
        layoutId = resolveLayout()
    }

    private fun resolveLayout(): Int {
        return when (DeviceInfo.getLayoutPreference()) {
            -1   -> if (DeviceInfo.isTVDevice()) TV else PHONE
            0    -> PHONE
            1    -> TV
            2    -> EMULATOR
            else -> PHONE
        }
    }
}
