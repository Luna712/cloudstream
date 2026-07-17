package com.lagradost.cloudstream4.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val LocalCloudStreamColors = staticCompositionLocalOf { darkScheme() }

object CloudStreamTheme {
    val colors: CloudStreamColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalCloudStreamColors.current
}

private fun CloudStreamColorScheme.toMaterial3ColorScheme() = if (isLight) {
    lightColorScheme(
        primary = primary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainer = surfaceContainer,
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary = Color.White,
    )
} else {
    darkColorScheme(
        primary = primary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainer = surfaceContainer,
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary = Color.White,
    )
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),

    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),

    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),

    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),

    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)

@Composable
fun CloudStreamTheme(
    mode: CloudStreamThemeMode = CloudStreamThemeMode.FollowSystem,
    primaryColor: CloudStreamPrimaryColor = CloudStreamPrimaryColor.NORMAL,
    fontSpec: CloudStreamFontSpec = CloudStreamDefaultFonts.GoogleSans,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dynamicTheme = resolveDynamicTheme()

    val dynamicPrimary = resolveDynamicPrimaryColor()
    val dynamicSecondary = resolveDynamicSecondaryColor()

    val csColors = remember(mode, primaryColor, systemDark, dynamicTheme, dynamicPrimary, dynamicSecondary) {
        val base = when (mode) {
            CloudStreamThemeMode.Dark -> darkScheme()
            CloudStreamThemeMode.Amoled -> amoledScheme()
            CloudStreamThemeMode.AmoledLight -> amoledLightScheme()
            CloudStreamThemeMode.Light -> lightScheme()
            CloudStreamThemeMode.Dracula -> draculaScheme()
            CloudStreamThemeMode.Lavender -> lavenderScheme()
            CloudStreamThemeMode.SilentBlue -> silentBlueScheme()
            CloudStreamThemeMode.FollowSystem -> if (systemDark) darkScheme() else lightScheme()
            CloudStreamThemeMode.Dynamic -> dynamicTheme
        }
        when {
            mode == CloudStreamThemeMode.Dynamic -> base
            primaryColor == CloudStreamPrimaryColor.DYNAMIC -> base.copy(primary = dynamicPrimary)
            primaryColor == CloudStreamPrimaryColor.DYNAMIC_TWO -> base.copy(primary = dynamicSecondary)
            else -> base.copy(primary = primaryColor.color)
        }
    }

    val resolvedFontFamily = fontSpec.resolve()
    val baseTypography = MaterialTheme.typography
    val typography = remember(baseTypography, resolvedFontFamily) {
        baseTypography.withFontFamily(resolvedFontFamily)
    }

    CompositionLocalProvider(LocalCloudStreamColors provides csColors) {
        MaterialTheme(
            colorScheme = csColors.toMaterial3ColorScheme(),
            typography = typography,
            content = content,
        )
    }
}
