package com.lagradost.cloudstream3.shared.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.DeviceLayout
import com.lagradost.cloudstream3.shared.generated.resources.Res
import com.lagradost.cloudstream3.shared.generated.resources.category_accounts
import com.lagradost.cloudstream3.shared.generated.resources.category_accounts_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.category_extensions
import com.lagradost.cloudstream3.shared.generated.resources.category_extensions_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.category_general
import com.lagradost.cloudstream3.shared.generated.resources.category_general_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.category_layout
import com.lagradost.cloudstream3.shared.generated.resources.category_layout_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.category_player
import com.lagradost.cloudstream3.shared.generated.resources.category_player_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.category_providers
import com.lagradost.cloudstream3.shared.generated.resources.category_providers_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.category_updates
import com.lagradost.cloudstream3.shared.generated.resources.category_updates_subtitle
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_blue
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_dark_blue
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_orange
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_pink
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_purple
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_red
import com.lagradost.cloudstream3.shared.generated.resources.profile_bg_teal
import com.lagradost.cloudstream3.shared.generated.resources.profile_picture_desc
import com.lagradost.cloudstream3.shared.ui.components.cloudStreamRipple
import com.lagradost.cloudstream3.shared.ui.components.settings.SettingsItem
import com.lagradost.cloudstream3.shared.ui.icons.account_circle
import com.lagradost.cloudstream3.shared.ui.icons.extension
import com.lagradost.cloudstream3.shared.ui.icons.mobile_arrow_down
import com.lagradost.cloudstream3.shared.ui.icons.palette
import com.lagradost.cloudstream3.shared.ui.icons.play_circle
import com.lagradost.cloudstream3.shared.ui.icons.storage
import com.lagradost.cloudstream3.shared.ui.icons.tune
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class ProfileImage {
    DARK_BLUE, BLUE, ORANGE, PINK, PURPLE, RED, TEAL;
}

data class SettingsProfileState(
    val name: String,
    val profilePictureUrl: String? = null,
    val profileImage: ProfileImage = ProfileImage.DARK_BLUE,
)

data class SettingsVersionState(
    val appVersion: String,
    val commitHash: String,
    val buildDate: String,
)

enum class SettingsCategory {
    GENERAL,
    PLAYER,
    PROVIDERS,
    LAYOUT,
    UPDATES,
    ACCOUNTS,
    EXTENSIONS,
}

@Composable
private fun ProfileImage.toRes() = when (this) {
    ProfileImage.DARK_BLUE -> Res.drawable.profile_bg_dark_blue
    ProfileImage.BLUE      -> Res.drawable.profile_bg_blue
    ProfileImage.ORANGE    -> Res.drawable.profile_bg_orange
    ProfileImage.PINK      -> Res.drawable.profile_bg_pink
    ProfileImage.PURPLE    -> Res.drawable.profile_bg_purple
    ProfileImage.RED       -> Res.drawable.profile_bg_red
    ProfileImage.TEAL      -> Res.drawable.profile_bg_teal
}

@Composable
fun SettingsCategory.label(): String = stringResource(
    when (this) {
        SettingsCategory.GENERAL    -> Res.string.category_general
        SettingsCategory.PLAYER     -> Res.string.category_player
        SettingsCategory.PROVIDERS  -> Res.string.category_providers
        SettingsCategory.LAYOUT     -> Res.string.category_layout
        SettingsCategory.UPDATES    -> Res.string.category_updates
        SettingsCategory.ACCOUNTS   -> Res.string.category_accounts
        SettingsCategory.EXTENSIONS -> Res.string.category_extensions
    }
)

@Composable
fun SettingsCategory.subtitle(): String = stringResource(
    when (this) {
        SettingsCategory.GENERAL    -> Res.string.category_general_subtitle
        SettingsCategory.PLAYER     -> Res.string.category_player_subtitle
        SettingsCategory.PROVIDERS  -> Res.string.category_providers_subtitle
        SettingsCategory.LAYOUT     -> Res.string.category_layout_subtitle
        SettingsCategory.UPDATES    -> Res.string.category_updates_subtitle
        SettingsCategory.ACCOUNTS   -> Res.string.category_accounts_subtitle
        SettingsCategory.EXTENSIONS -> Res.string.category_extensions_subtitle
    }
)

private fun SettingsCategory.icon(): ImageVector = when (this) {
    SettingsCategory.GENERAL    -> tune
    SettingsCategory.PLAYER     -> play_circle
    SettingsCategory.PROVIDERS  -> storage
    SettingsCategory.LAYOUT     -> palette
    SettingsCategory.UPDATES    -> mobile_arrow_down
    SettingsCategory.ACCOUNTS   -> account_circle
    SettingsCategory.EXTENSIONS -> extension
}

@Composable
fun SettingsScreen(
    profile: SettingsProfileState,
    version: SettingsVersionState,
    avatarContent: @Composable () -> Unit,
    onNavigate: (SettingsCategory) -> Unit,
    onVersionLongClick: () -> Unit = {},
) {
    val colors = CloudStreamTheme.colors
    val isTV = remember { DeviceLayout.isLayout(DeviceLayout.TV) }
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (isTV) firstItemFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
                if (isTV) Modifier.windowInsetsPadding(
                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                ) else Modifier
            )
            .verticalScroll(rememberScrollState())
    ) {
        SettingsProfileHeader(profile = profile, avatarContent = avatarContent)

        SettingsCategory.entries.forEachIndexed { index, category ->
            SettingsItem(
                title = category.label(),
                subtitle = category.subtitle(),
                icon = category.icon(),
                focusRequester = if (index == 0) firstItemFocusRequester else null,
                onClick = { onNavigate(category) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsVersionFooter(version = version, onLongClick = onVersionLongClick)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsProfileHeader(
    profile: SettingsProfileState,
    avatarContent: @Composable () -> Unit,
) {
    val colors = CloudStreamTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .border(1.dp, colors.onBackground.copy(alpha = 0.2f), CircleShape)
                .clip(CircleShape),
        ) {
            if (profile.profilePictureUrl != null) {
                avatarContent()
            } else {
                Image(
                    painter = painterResource(profile.profileImage.toRes()),
                    contentDescription = stringResource(Res.string.profile_picture_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = profile.name,
            color = colors.onBackground,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsVersionFooter(version: SettingsVersionState, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = {},
            )
            .cloudStreamRipple(interactionSource)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VersionChip(version.appVersion)
        VersionDot()
        VersionChip(version.commitHash)
        VersionDot()
        VersionChip(version.buildDate)
    }
}

@Composable
private fun VersionChip(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        color = CloudStreamTheme.colors.onBackground.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun VersionDot() {
    Text(
        text = "•",
        color = CloudStreamTheme.colors.onBackground.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodySmall,
    )
}
