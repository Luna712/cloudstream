package com.lagradost.cloudstream3.shared

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.preference.PreferenceManager
import com.lagradost.api.getContext
import com.lagradost.cloudstream3.shared.preferences.PreferenceKeys

internal actual object DeviceInfo {
    actual const val UI_MODE_TELEVISION: Int = Configuration.UI_MODE_TYPE_TELEVISION

    actual fun getModel(): String = Build.MODEL

    actual fun getUIMode(): Int {
        val context = getContext() as? Context ?: return 0
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?
        return uiModeManager?.currentModeType ?: 0
    }

    actual fun getLayoutPreference(): Int {
        val context = getContext() as? Context ?: return -1
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(PreferenceKeys.APP_LAYOUT, -1)
    }
}
