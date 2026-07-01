package com.lagradost.cloudstream4.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.ui.theme.CloudStreamPalette
import com.lagradost.cloudstream4.ui.theme.CloudStreamTheme
import com.lagradost.cloudstream4.ui.theme.CloudStreamThemeMode
import kotlin.test.Test
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class CloudStreamRippleTest {

    @Test
    fun appliesSuccessfullyWhenBounded() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        setContent {
            CloudStreamTheme {
                Box(
                    modifier = Modifier
                        .testTag("target")
                        .size(64.dp)
                        .cloudStreamRipple(interactionSource, bounded = true),
                )
            }
        }
        waitForIdle()

        onNodeWithTag("target").assertIsDisplayed()
    }

    @Test
    fun appliesSuccessfullyWhenUnbounded() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        setContent {
            CloudStreamTheme {
                Box(
                    modifier = Modifier
                        .testTag("target")
                        .size(64.dp)
                        .cloudStreamRipple(interactionSource, bounded = false),
                )
            }
        }
        waitForIdle()

        onNodeWithTag("target").assertIsDisplayed()
    }

    @Test
    fun appliesSuccessfullyUnderEveryThemeMode() = runComposeUiTest {
        val modes = CloudStreamThemeMode.entries.filterNot { it == CloudStreamThemeMode.Dynamic }

        setContent {
            Column {
                modes.forEach { mode ->
                    val interactionSource = remember { MutableInteractionSource() }
                    CloudStreamTheme(mode = mode) {
                        Box(
                            modifier = Modifier
                                .testTag("target-${mode.name}")
                                .size(32.dp)
                                .cloudStreamRipple(interactionSource),
                        )
                    }
                }
            }
        }
        waitForIdle()

        modes.forEach { mode ->
            onNodeWithTag("target-${mode.name}").assertIsDisplayed()
        }
    }

    /**
     * Check rhat it emits a real [PressInteraction.Press] into the
     * [MutableInteractionSource] and confirms the pixel under the press point
     * actually changes once the ripple draws over it.
     *
     * This test may end up being flaky. If so, it can be dropped.
     */
    @Test
    fun pressInteractionChangesThePixelsUnderThePress() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val boxSizeDp = 80

        setContent {
            CloudStreamTheme(mode = CloudStreamThemeMode.Dark) {
                Box(
                    modifier = Modifier
                        .testTag("target")
                        .size(boxSizeDp.dp)
                        .background(CloudStreamPalette.DarkBlackBg)
                        .cloudStreamRipple(interactionSource),
                )
            }
        }
        waitForIdle()

        val target = onNodeWithTag("target")
        val idlePixels = target.captureToImage().toPixelMap()
        val centerX = idlePixels.width / 2
        val centerY = idlePixels.height / 2
        val idleCenterColor = idlePixels[centerX, centerY]

        val press = PressInteraction.Press(Offset(centerX.toFloat(), centerY.toFloat()))
        interactionSource.tryEmit(press)
        waitForIdle()

        val pressedPixels = target.captureToImage().toPixelMap()
        val pressedCenterColor = pressedPixels[centerX, centerY]

        assertNotEquals(
            idleCenterColor,
            pressedCenterColor,
            "Expected the ripple to visibly change the pixel under the press point",
        )

        // Release cleanly so the ripple doesn't leak a running animation past the test.
        interactionSource.tryEmit(PressInteraction.Release(press))
        waitForIdle()
    }
}
