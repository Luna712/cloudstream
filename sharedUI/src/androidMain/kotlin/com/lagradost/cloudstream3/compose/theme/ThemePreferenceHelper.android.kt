package com.lagradost.cloudstream3.shared.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.shared.preferences.PreferenceKeys

fun Context.loadCloudStreamThemeMode(): CloudStreamThemeMode {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return when (prefs.getString(PreferenceKeys.APP_THEME, "AmoledLight")) {
        "System"      -> CloudStreamThemeMode.FollowSystem
        "Black"       -> CloudStreamThemeMode.Dark
        "Light"       -> CloudStreamThemeMode.Light
        "Amoled"      -> CloudStreamThemeMode.Amoled
        "AmoledLight" -> CloudStreamThemeMode.Amoled
        "Dracula"     -> CloudStreamThemeMode.Dracula
        "Lavender"    -> CloudStreamThemeMode.Lavender
        "SilentBlue"  -> CloudStreamThemeMode.SilentBlue
        "Monet"       -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                             CloudStreamThemeMode.Monet
                         else CloudStreamThemeMode.Dark
        else          -> CloudStreamThemeMode.Dark
    }
}

fun Context.loadPrimaryColor(): CloudStreamPrimaryColor {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    return when (prefs.getString(PreferenceKeys.PRIMARY_COLOR, "Normal")) {
        "Normal"          -> CloudStreamPrimaryColor.NORMAL
        "Blue"            -> CloudStreamPrimaryColor.BLUE
        "Purple"          -> CloudStreamPrimaryColor.PURPLE
        "Green"           -> CloudStreamPrimaryColor.GREEN
        "GreenApple"      -> CloudStreamPrimaryColor.GREEN_APPLE
        "Red"             -> CloudStreamPrimaryColor.RED
        "Banana"          -> CloudStreamPrimaryColor.BANANA
        "Party"           -> CloudStreamPrimaryColor.PARTY
        "Pink"            -> CloudStreamPrimaryColor.PINK
        "CarnationPink"   -> CloudStreamPrimaryColor.CARNATION_PINK
        "Maroon"          -> CloudStreamPrimaryColor.MAROON
        "DarkGreen"       -> CloudStreamPrimaryColor.DARK_GREEN
        "NavyBlue"        -> CloudStreamPrimaryColor.NAVY_BLUE
        "Grey"            -> CloudStreamPrimaryColor.GREY
        "White"           -> CloudStreamPrimaryColor.WHITE
        "Brown"           -> CloudStreamPrimaryColor.BROWN
        "Orange"          -> CloudStreamPrimaryColor.ORANGE
        "DandelionYellow" -> CloudStreamPrimaryColor.DANDELION_YELLOW
        "CoolBlue"        -> CloudStreamPrimaryColor.COOL_BLUE
        "Lavender"        -> CloudStreamPrimaryColor.LAVENDER
        "Monet"           -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                 CloudStreamPrimaryColor.MONET
                             else CloudStreamPrimaryColor.NORMAL
        "Monet2"          -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                 CloudStreamPrimaryColor.MONET_TWO
                             else CloudStreamPrimaryColor.NORMAL
        else              -> CloudStreamPrimaryColor.NORMAL
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun Context.buildMonetScheme(): CloudStreamColorScheme {
    CloudStreamColorScheme(
        background       = Color(getColor(android.R.color.system_neutral1_900)),
        surfaceVariant   = Color(getColor(android.R.color.system_neutral1_800)),
        surface          = Color(getColor(android.R.color.system_neutral1_800)),
        surfaceContainer = Color(getColor(android.R.color.system_neutral1_800)),
        onBackground     = Color(getColor(android.R.color.system_neutral1_100)),
        onSurfaceVariant = Color(getColor(android.R.color.system_neutral2_400)),
        icon             = Color(getColor(android.R.color.system_neutral1_100)),
        primary          = Color(getColor(android.R.color.system_accent1_200)),
        ongoing          = CloudStreamPalette.Ongoing,
        isLight          = false,
    )
}
