package com.lagradost.cloudstream3.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

val LocalCloudStreamColors = staticCompositionLocalOf { darkScheme() }

object CloudStreamTheme {
    val colors: CloudStreamColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalCloudStreamColors.current
}

private fun CloudStreamColorScheme.toMaterial3ColorScheme() = if (isLight) {
    lightColorScheme(
        primary          = primary,
        background       = background,
        surface          = surface,
        surfaceVariant   = surfaceVariant,
        surfaceContainer = surfaceContainer,
        onBackground     = onBackground,
        onSurface        = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary        = Color.White,
    )
} else {
    darkColorScheme(
        primary          = primary,
        background       = background,
        surface          = surface,
        surfaceVariant   = surfaceVariant,
        surfaceContainer = surfaceContainer,
        onBackground     = onBackground,
        onSurface        = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary        = Color.White,
    )
}

@Composable
fun CloudStreamTheme(
    mode: CloudStreamThemeMode = CloudStreamThemeMode.FollowSystem,
    primaryColor: CloudStreamPrimaryColor = CloudStreamPrimaryColor.NORMAL,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dynamicTheme = resolveDynamicTheme()

    val csColors = remember(mode, primaryColor, systemDark, dynamicTheme) {
        val base = when (mode) {
            CloudStreamThemeMode.Dark         -> darkScheme()
            CloudStreamThemeMode.Amoled       -> amoledScheme()
            CloudStreamThemeMode.Light        -> lightScheme()
            CloudStreamThemeMode.Dracula      -> draculaScheme()
            CloudStreamThemeMode.Lavender     -> lavenderScheme()
            CloudStreamThemeMode.SilentBlue   -> silentBlueScheme()
            CloudStreamThemeMode.FollowSystem -> if (systemDark) darkScheme() else lightScheme()
            CloudStreamThemeMode.Dynamic      -> dynamicTheme
        }
        if (mode == CloudStreamThemeMode.Dynamic) base
        else base.copy(primary = primaryColor.color)
    }

    CompositionLocalProvider(LocalCloudStreamColors provides csColors) {
        MaterialTheme(
            colorScheme = csColors.toMaterial3ColorScheme(),
            content = content,
        )
    }
}
