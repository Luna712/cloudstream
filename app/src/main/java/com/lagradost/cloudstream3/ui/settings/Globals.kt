package com.lagradost.cloudstream3.ui.settings

import android.content.Context
import com.lagradost.cloudstream4.utils.DeviceLayout

object Globals {
    var beneneCount = 0

    val PHONE: Int = DeviceLayout.PHONE.value
    val TV: Int = DeviceLayout.TV.value
    val EMULATOR: Int = DeviceLayout.EMULATOR.value

    fun Context.updateTv() {
        DeviceLayout.update()
    }

    fun isLandscape(): Boolean = DeviceLayout.isLandscape()

    fun isLayout(flags: Int): Boolean = DeviceLayout.isLayout(DeviceLayout.Layout(flags))
}
