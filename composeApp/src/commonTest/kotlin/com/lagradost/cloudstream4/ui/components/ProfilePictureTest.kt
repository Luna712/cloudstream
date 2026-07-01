package com.lagradost.cloudstream4.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.ui.theme.CloudStreamTheme
import kotlin.test.Test

/**
 * The content description ("Profile picture") comes from
 * `Res.string.profile_picture_desc`, see composeResources/values/strings.xml.
 */
@OptIn(ExperimentalTestApi::class)
class ProfilePictureTest {

    @Test
    fun rendersLocalFallbackImageWhenNoUrlIsProvided() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                ProfilePicture(profileImage = ProfileImage.BLUE)
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Profile picture").assertIsDisplayed()
    }

    @Test
    fun rendersRemoteImageWhenUrlIsProvided() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                ProfilePicture(
                    profileImage = ProfileImage.TEAL,
                    profilePictureUrl = "https://example.com/avatar.png",
                )
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Profile picture").assertIsDisplayed()
    }

    @Test
    fun usesTheDefaultFiftyDpSizeWhenNotOverridden() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                ProfilePicture(profileImage = ProfileImage.PURPLE)
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Profile picture")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun respectsACustomSize() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                ProfilePicture(profileImage = ProfileImage.PINK, size = 96.dp)
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Profile picture")
            .assertWidthIsEqualTo(96.dp)
            .assertHeightIsEqualTo(96.dp)
    }

    @Test
    fun rendersForEveryProfileImageVariantWithoutCrashing() = runComposeUiTest {
        setContent {
            CloudStreamTheme {
                Column {
                    for (variant in ProfileImage.entries) {
                        ProfilePicture(profileImage = variant)
                    }
                }
            }
        }
        waitForIdle()

        onAllNodesWithContentDescription("Profile picture")
            .assertCountEquals(ProfileImage.entries.size)
    }
}
