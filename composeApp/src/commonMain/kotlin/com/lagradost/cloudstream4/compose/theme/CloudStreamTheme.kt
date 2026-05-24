package com.lagradost.cloudstream4.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lagradost.cloudstream4.compose.toast.CloudStreamSnackbar
import com.lagradost.cloudstream4.compose.toast.ToastEffectHost

val LocalCloudStreamColors = staticCompositionLocalOf { darkScheme() }
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

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

@Composable
fun CloudStreamTheme(
    mode: CloudStreamThemeMode = CloudStreamThemeMode.FollowSystem,
    primaryColor: CloudStreamPrimaryColor = CloudStreamPrimaryColor.NORMAL,
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

    val parentHostState = LocalSnackbarHostState.current
    val isRoot = parentHostState == null
    val hostState = remember { parentHostState ?: SnackbarHostState() }

    if (isRoot) ToastEffectHost(hostState)

    CompositionLocalProvider(
        LocalCloudStreamColors provides csColors,
        LocalSnackbarHostState provides hostState,
    ) {
        MaterialTheme(colorScheme = csColors.toMaterial3ColorScheme()) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                if (isRoot) {
                    SnackbarHost(
                        hostState = hostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                        snackbar = { CloudStreamSnackbar(it) },
                    )
                }
            }
        }
    }
}
