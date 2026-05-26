package com.lagradost.cloudstream3.ui.settings

import android.content.Context
import com.lagradost.cloudstream4.utils.DeviceLayout

object Globals {
    var beneneCount = 0

    val PHONE = DeviceLayout.PHONE
    val TV = DeviceLayout.TV
    val EMULATOR = DeviceLayout.EMULATOR

    fun Context.updateTv() {
        DeviceLayout.update()
    }

    fun isLandscape(): Boolean = DeviceLayout.isLandscape()

    fun isLayout(flags: DeviceLayout.Layout): Boolean = DeviceLayout.isLayout(flags)
}
