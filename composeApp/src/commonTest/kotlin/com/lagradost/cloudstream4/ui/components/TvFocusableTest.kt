package com.lagradost.cloudstream4.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the two distinct interaction models of [Modifier.tvFocusable]:
 *  - Phone/touch (`isTV = false`): behaves like a normal clickable, one tap = one click.
 *  - TV (`isTV = true`): the first tap only requests focus, a second tap while
 *    focused is what actually invokes [onClick] (mirrors D-pad "select" behavior).
 */
@OptIn(ExperimentalTestApi::class)
class TvFocusableTest {

    @Test
    fun nonTvModeInvokesOnClickOnASingleTap() = runComposeUiTest {
        var clickCount = 0
        setContent {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("target")
                    .tvFocusable(isTV = false, onClick = { clickCount++ }),
            )
        }

        onNodeWithTag("target").performClick()
        waitForIdle()

        assertEquals(1, clickCount)
    }

    @Test
    fun nonTvModeInvokesOnClickOncePerTap() = runComposeUiTest {
        var clickCount = 0
        setContent {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("target")
                    .tvFocusable(isTV = false, onClick = { clickCount++ }),
            )
        }

        val target = onNodeWithTag("target")
        target.performClick()
        target.performClick()
        target.performClick()
        waitForIdle()

        assertEquals(3, clickCount)
    }

    @Test
    fun tvModeFirstTapOnlyRequestsFocusAndDoesNotInvokeOnClick() = runComposeUiTest {
        var clickCount = 0
        setContent {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("target")
                    .tvFocusable(isTV = true, onClick = { clickCount++ }),
            )
        }

        onNodeWithTag("target").performTouchInput { click() }
        waitForIdle()

        assertEquals(0, clickCount)
    }

    @Test
    fun tvModeSecondTapWhileFocusedInvokesOnClick() = runComposeUiTest {
        var clickCount = 0
        setContent {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("target")
                    .tvFocusable(isTV = true, onClick = { clickCount++ }),
            )
        }

        val target = onNodeWithTag("target")
        target.performTouchInput { click() } // 1st tap: gains focus
        waitForIdle()
        target.performTouchInput { click() } // 2nd tap: now focused, should click
        waitForIdle()

        assertEquals(1, clickCount)
    }

    @Test
    fun tvModeReportsFocusChangesThroughCallback() = runComposeUiTest {
        var focused = false
        setContent {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("target")
                    .tvFocusable(
                        isTV = true,
                        onClick = {},
                        onFocusChanged = { focused = it },
                    ),
            )
        }

        onNodeWithTag("target").performTouchInput { click() }
        waitForIdle()

        assertTrue(focused, "Expected the element to report focus after the first tap")
    }
}
