package com.lagradost.cloudstream4.utils

import com.lagradost.cloudstream4.preferences.PreferenceDefaults
import java.awt.Toolkit

internal actual object DeviceInfo {
    actual fun getDeviceLayout(): Int = DeviceLayout.COMPUTER

    actual fun isLandscape(): Boolean {
        return try {
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            screenSize.width > screenSize.height
        } catch (_: Exception) {
            true // Assume landscape as that is more likely on JVM
        }
    }

    actual fun getLayoutPreference(): Int = PreferenceDefaults.APP_LAYOUT
}
