package com.lagradost.cloudstream3.compose.theme

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R

fun Context.loadCloudStreamThemeMode(): CloudStreamThemeMode {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return when (prefs.getString(getString(R.string.app_theme_key), "AmoledLight")) {
        "System"      -> CloudStreamThemeMode.FollowSystem
        "Black"       -> CloudStreamThemeMode.Dark
        "Light"       -> CloudStreamThemeMode.Light
        "Amoled"      -> CloudStreamThemeMode.Amoled
        "AmoledLight" -> CloudStreamThemeMode.Amoled
        "Dracula"     -> CloudStreamThemeMode.Dracula
        "Lavender"    -> CloudStreamThemeMode.Lavender
        "SilentBlue"  -> CloudStreamThemeMode.SilentBlue
        // Monet requires API 31+ dynamic colors; fall back to system for now
        "Monet"       -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                             CloudStreamThemeMode.FollowSystem
                         else CloudStreamThemeMode.Dark
        else          -> CloudStreamThemeMode.Dark
    }
}
