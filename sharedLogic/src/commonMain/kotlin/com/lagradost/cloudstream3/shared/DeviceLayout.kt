package com.lagradost.cloudstream3.shared

object DeviceLayout {
    const val PHONE    : Int = 0b001
    const val TV       : Int = 0b010
    const val EMULATOR : Int = 0b100

    private val layoutId: Int get() = resolveLayout()

    fun isLayout(flags: Int): Boolean = (layoutId and flags) != 0

    private fun resolveLayout(): Int {
        return when (DeviceInfo.getLayoutPreference()) {
            -1   -> if (isAutoTv()) TV else PHONE
            0    -> PHONE
            1    -> TV
            2    -> EMULATOR
            else -> PHONE
        }
    }

    private fun isAutoTv(): Boolean {
        val model = DeviceInfo.getModel().lowercase()
        return DeviceInfo.getUIMode() == DeviceInfo.UI_MODE_TELEVISION
            || DeviceInfo.getModel().contains("AFT")
            || model.contains("firestick")
            || model.contains("fire tv")
            || model.contains("chromecast")
    }
}
