package com.lagradost.cloudstream4.ui.components.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.lagradost.cloudstream4.ui.icons.tune
import com.lagradost.cloudstream4.ui.theme.CloudStreamTheme
import com.lagradost.cloudstream4.utils.DeviceLayout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsItemTest {

    @BeforeTest
    fun resolveDeviceLayout() {
        DeviceLayout.update()
    }

    @Test
    fun displaysTitleAndSubtitleWhenBothAreProvided() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                SettingsItem(
                    title = "My Setting",
                    subtitle = "A helpful subtitle",
                    onClick = {},
                )
            }
        }
        waitForIdle()

        onNodeWithText("My Setting").assertIsDisplayed()
        onNodeWithText("A helpful subtitle").assertIsDisplayed()
    }

    @Test
    fun omitsTheSubtitleNodeWhenSubtitleIsNull() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                SettingsItem(title = "Only Title", subtitle = null, onClick = {})
            }
        }
        waitForIdle()

        onNodeWithText("Only Title").assertIsDisplayed()
        onAllNodesWithText("A helpful subtitle").assertCountEquals(0)
    }

    @Test
    fun clickingAnywhereOnTheRowInvokesOnClick() = runComposeUiTest {
        var clickCount = 0
        setContent {
            CloudStreamTheme {
                SettingsItem(title = "Clickable Setting", onClick = { clickCount++ })
            }
        }

        onNodeWithText("Clickable Setting").performClick()
        waitForIdle()

        assertEquals(1, clickCount)
    }

    @Test
    fun rendersSuccessfullyWithAnIconAttached() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                SettingsItem(title = "With Icon", subtitle = "Has an icon", icon = tune, onClick = {})
            }
        }
        waitForIdle()

        onNodeWithText("With Icon").assertIsDisplayed()
        onNodeWithText("Has an icon").assertIsDisplayed()
    }

    @Test
    fun rendersSuccessfullyWithoutAnIcon() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                SettingsItem(title = "No Icon Here", icon = null, onClick = {})
            }
        }
        waitForIdle()

        onNodeWithText("No Icon Here").assertIsDisplayed()
    }

    @Test
    fun multipleClicksEachInvokeOnClick() = runComposeUiTest {
        var clickCount = 0
        setContent {
            CloudStreamTheme {
                SettingsItem(title = "Repeatable", onClick = { clickCount++ })
            }
        }

        val node = onNodeWithText("Repeatable")
        node.performClick()
        node.performClick()
        node.performClick()
        waitForIdle()

        assertTrue(clickCount == 3, "Expected 3 clicks but got $clickCount")
    }
}
