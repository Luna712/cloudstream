package com.lagradost.cloudstream4.ui.screens.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.lagradost.cloudstream4.ui.components.ProfileImage
import com.lagradost.cloudstream4.ui.theme.CloudStreamTheme
import com.lagradost.cloudstream4.utils.DeviceLayout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @BeforeTest
    fun resolveDeviceLayout() {
        DeviceLayout.update()
    }

    private val profile = SettingsProfileState(name = "Test User", profileImage = ProfileImage.DARK_BLUE)
    private val version = SettingsVersionState(
        appVersion = "1.0.0-PRE",
        commitHash = "abc1234",
        buildDate = "Jan 1, 2026",
    )

    @Test
    fun displaysTheProfileName() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                SettingsScreen(profile = profile, version = version, onNavigate = {})
            }
        }
        waitForIdle()

        onNodeWithText("Test User").assertIsDisplayed()
    }

    @Test
    fun displaysEveryCategoryTitleAndSubtitle() = runComposeUiTest {
        // Resolve the localized strings from within the same composition so this
        // test stays correct even if the copy in strings.xml changes.
        lateinit var expected: List<Pair<String, String>>

        setContent {
            expected = SettingsCategory.entries.map { it.label() to it.subtitle() }
            CloudStreamTheme {
                SettingsScreen(profile = profile, version = version, onNavigate = {})
            }
        }
        waitForIdle()

        expected.forEach { (title, subtitle) ->
            onNodeWithText(title).assertIsDisplayed()
            onNodeWithText(subtitle).assertIsDisplayed()
        }
    }

    @Test
    fun clickingACategoryInvokesOnNavigateWithThatCategory() = runComposeUiTest {
        val navigated = mutableListOf<SettingsCategory>()
        lateinit var playerTitle: String

        setContent {
            playerTitle = SettingsCategory.PLAYER.label()
            CloudStreamTheme {
                SettingsScreen(
                    profile = profile,
                    version = version,
                    onNavigate = { navigated += it },
                )
            }
        }
        waitForIdle()

        onNodeWithText(playerTitle).performClick()
        waitForIdle()

        assertEquals(listOf(SettingsCategory.PLAYER), navigated)
    }

    @Test
    fun clickingEachCategoryNavigatesExactlyOnce() = runComposeUiTest {
        val navigated = mutableListOf<SettingsCategory>()
        lateinit var titlesInOrder: List<Pair<SettingsCategory, String>>

        setContent {
            titlesInOrder = SettingsCategory.entries.map { it to it.label() }
            CloudStreamTheme {
                SettingsScreen(
                    profile = profile,
                    version = version,
                    onNavigate = { navigated += it },
                )
            }
        }
        waitForIdle()

        titlesInOrder.forEach { (_, title) -> onNodeWithText(title).performClick() }
        waitForIdle()

        assertEquals(titlesInOrder.map { it.first }, navigated)
    }

    @Test
    fun displaysAllThreeVersionChips() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                SettingsScreen(profile = profile, version = version, onNavigate = {})
            }
        }
        waitForIdle()

        onNodeWithText(version.appVersion).assertIsDisplayed()
        onNodeWithText(version.commitHash).assertIsDisplayed()
        onNodeWithText(version.buildDate).assertIsDisplayed()
    }

    @Test
    fun longPressingTheVersionFooterInvokesOnVersionLongClick() = runComposeUiTest {
        var longClicked = false
        setContent {
            CloudStreamTheme {
                SettingsScreen(
                    profile = profile,
                    version = version,
                    onNavigate = {},
                    onVersionLongClick = { longClicked = true },
                )
            }
        }
        waitForIdle()

        onNodeWithText(version.appVersion).performTouchInput { longClick() }
        waitForIdle()

        assertTrue(longClicked, "Expected long-pressing the version footer to fire onVersionLongClick")
    }

    @Test
    fun shortPressingTheVersionFooterDoesNotInvokeOnVersionLongClick() = runComposeUiTest {
        var longClicked = false
        setContent {
            CloudStreamTheme {
                SettingsScreen(
                    profile = profile,
                    version = version,
                    onNavigate = {},
                    onVersionLongClick = { longClicked = true },
                )
            }
        }
        waitForIdle()

        onNodeWithText(version.appVersion).performClick()
        waitForIdle()

        assertTrue(!longClicked)
    }
}
