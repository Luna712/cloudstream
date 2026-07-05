package com.lagradost.cloudstream4.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

@Composable
actual fun resolveDynamicTheme(): CloudStreamColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        buildMonetScheme()
    else darkScheme()
}

@Composable
private fun buildMonetScheme(): CloudStreamColorScheme {
    val isSystemDark = isSystemInDarkTheme()
    return if (isSystemDark) {
        CloudStreamColorScheme(
            background = colorResource(android.R.color.system_neutral1_900),
            surfaceVariant = colorResource(android.R.color.system_neutral1_800),
            surface = colorResource(android.R.color.system_neutral1_800),
            surfaceContainer = colorResource(android.R.color.system_neutral1_800),
            onBackground = colorResource(android.R.color.system_neutral1_100),
            onSurfaceVariant = colorResource(android.R.color.system_neutral2_400),
            icon = colorResource(android.R.color.system_neutral1_100),
            primary = colorResource(android.R.color.system_accent1_200),
            ongoing = CloudStreamPalette.Ongoing,
            isLight = false,
        )
    } else {
        CloudStreamColorScheme(
            background = colorResource(android.R.color.system_neutral1_10),
            surfaceVariant = colorResource(android.R.color.system_neutral1_100),
            surface = colorResource(android.R.color.system_neutral1_100),
            surfaceContainer = colorResource(android.R.color.system_neutral1_100),
            onBackground = colorResource(android.R.color.system_neutral1_900),
            onSurfaceVariant = colorResource(android.R.color.system_neutral2_600),
            icon = colorResource(android.R.color.system_neutral1_900),
            primary = colorResource(android.R.color.system_accent1_600),
            ongoing = CloudStreamPalette.Ongoing,
            isLight = true,
        )
    }
}
