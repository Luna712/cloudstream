package com.lagradost.cloudstream3.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.generated.resources.Res
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_blue
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_dark_blue
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_orange
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_pink
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_purple
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_red
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_teal
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class ProfileImage {
    DARK_BLUE, BLUE, ORANGE, PINK, PURPLE, RED, TEAL;
}

@Composable
private fun ProfileImage.toRes(): DrawableResource = when (this) {
    ProfileImage.DARK_BLUE -> Res.drawable.profile_bg_dark_blue
    ProfileImage.BLUE      -> Res.drawable.profile_bg_blue
    ProfileImage.ORANGE    -> Res.drawable.profile_bg_orange
    ProfileImage.PINK      -> Res.drawable.profile_bg_pink
    ProfileImage.PURPLE    -> Res.drawable.profile_bg_purple
    ProfileImage.RED       -> Res.drawable.profile_bg_red
    ProfileImage.TEAL      -> Res.drawable.profile_bg_teal
}

/**
 * Circular profile picture component.
 *
 * Shows [avatarContent] if [profilePictureUrl] is not null,
 * otherwise falls back to the local [profileImage] background.
 *
 * @param profileImage Local background image fallback
 * @param size Diameter of the circle, default 50.dp
 * @param profilePictureUrl Optional URL, if present [avatarContent] is shown
 * @param avatarContent Platform-provided image loader (e.g. Coil AsyncImage on Android)
 */
@Composable
fun ProfilePicture(
    profileImage: ProfileImage,
    size: Dp = 50.dp,
    profilePictureUrl: String? = null,
    avatarContent: (@Composable () -> Unit)? = null,
) {
    val colors = CloudStreamTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .border(2.dp, colors.onBackground.copy(alpha = 0.2f), CircleShape)
            .clip(CircleShape),
    ) {
        if (avatarContent != null && profilePictureUrl != null) {
            avatarContent()
        } else {
            Image(
                painter = painterResource(profileImage.toRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
