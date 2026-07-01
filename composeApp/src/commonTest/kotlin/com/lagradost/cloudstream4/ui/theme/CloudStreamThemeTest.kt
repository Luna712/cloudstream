package com.lagradost.cloudstream4.ui.theme

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that [CloudStreamTheme] resolves the right [CloudStreamColorScheme]
 * for each [CloudStreamThemeMode], and that explicit primary color overrides apply.
 */
@OptIn(ExperimentalTestApi::class)
class CloudStreamThemeTest {

    @Test
    fun darkModeExposesADarkColorScheme() = runComposeUiTest {
        lateinit var colors: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.Dark) {
                colors = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertFalse(colors.isLight)
        assertEquals(CloudStreamPalette.DarkBlackBg, colors.background)
    }

    @Test
    fun lightModeExposesALightColorScheme() = runComposeUiTest {
        lateinit var colors: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.Light) {
                colors = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertTrue(colors.isLight)
        assertEquals(CloudStreamPalette.LightBlackBg, colors.background)
    }

    @Test
    fun amoledModeUsesPureBlackForEveryBackgroundLayer() = runComposeUiTest {
        lateinit var colors: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.Amoled) {
                colors = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertEquals(CloudStreamPalette.AmoledBlack, colors.background)
        assertEquals(CloudStreamPalette.AmoledBlack, colors.surface)
        assertEquals(CloudStreamPalette.AmoledBlack, colors.surfaceVariant)
        assertEquals(CloudStreamPalette.AmoledBlack, colors.surfaceContainer)
    }

    @Test
    fun draculaModeExposesTheDraculaScheme() = runComposeUiTest {
        lateinit var colors: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.Dracula) {
                colors = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertEquals(CloudStreamPalette.DraculaBlackBg, colors.background)
        assertFalse(colors.isLight)
    }

    @Test
    fun explicitPrimaryColorOverridesTheSchemeDefaultPrimary() = runComposeUiTest {
        lateinit var colors: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(
                mode = CloudStreamThemeMode.Dark,
                primaryColor = CloudStreamPrimaryColor.RED,
            ) {
                colors = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertEquals(CloudStreamPrimaryColor.RED.color, colors.primary)
    }

    @Test
    fun defaultPrimaryColorMatchesNormal() = runComposeUiTest {
        lateinit var colors: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.Dark) {
                colors = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertEquals(CloudStreamPrimaryColor.NORMAL.color, colors.primary)
    }

    @Test
    fun colorsAreProvidedThroughLocalCloudStreamColors() = runComposeUiTest {
        lateinit var fromLocal: CloudStreamColorScheme
        lateinit var fromObject: CloudStreamColorScheme
        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.SilentBlue) {
                fromLocal = LocalCloudStreamColors.current
                fromObject = CloudStreamTheme.colors
            }
        }
        waitForIdle()

        assertEquals(fromLocal.background, fromObject.background)
        assertEquals(CloudStreamPalette.SilentBlueBlackBg, fromLocal.background)
    }
}
