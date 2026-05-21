package com.lagradost.cloudstream3.compose.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.compose.generated.resources.Res
import com.lagradost.cloudstream3.compose.generated.resources.category_account
import com.lagradost.cloudstream3.compose.generated.resources.category_general
import com.lagradost.cloudstream3.compose.generated.resources.category_player
import com.lagradost.cloudstream3.compose.generated.resources.category_providers
import com.lagradost.cloudstream3.compose.generated.resources.category_ui
import com.lagradost.cloudstream3.compose.generated.resources.category_updates
import com.lagradost.cloudstream3.compose.generated.resources.extensions
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_blue
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_dark_blue
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_orange
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_pink
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_purple
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_red
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_teal
import com.lagradost.cloudstream3.compose.theme.CloudStreamTheme
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
    UI,
    UPDATES,
    ACCOUNT,
    EXTENSIONS,
}

@Composable
fun SettingsCategory.label(): String = stringResource(
    when (this) {
        SettingsCategory.GENERAL    -> Res.string.category_general
        SettingsCategory.PLAYER     -> Res.string.category_player
        SettingsCategory.PROVIDERS  -> Res.string.category_providers
        SettingsCategory.UI         -> Res.string.category_ui
        SettingsCategory.UPDATES    -> Res.string.category_updates
        SettingsCategory.ACCOUNT    -> Res.string.category_account
        SettingsCategory.EXTENSIONS -> Res.string.extensions
    }
)

private fun SettingsCategory.icon(): ImageVector = when (this) {
    SettingsCategory.GENERAL    -> Icons.Outlined.Tune
    SettingsCategory.PLAYER     -> Icons.Outlined.PlayCircle
    SettingsCategory.PROVIDERS  -> Icons.Outlined.Storage
    SettingsCategory.UI         -> Icons.Outlined.Palette
    SettingsCategory.UPDATES    -> Icons.Outlined.SystemUpdate
    SettingsCategory.ACCOUNT    -> Icons.Outlined.AccountCircle
    SettingsCategory.EXTENSIONS -> Icons.Outlined.Extension
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsProfileHeader(profile = profile, avatarContent = avatarContent)

        Spacer(modifier = Modifier.height(8.dp))

        SettingsCategory.entries.forEach { category ->
            SettingsCategoryRow(
                label = category.label(),
                icon = category.icon(),
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
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            if (profile.profilePictureUrl != null) {
                avatarContent()
            } else {
                Image(
                    painter = painterResource(profile.profileImage.toRes()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = profile.name,
            color = colors.onBackground,
            fontSize = 18.sp,
        )
    }
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
private fun SettingsCategoryRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val colors = CloudStreamTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = colors.onBackground,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier.size(16.dp),
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = colors.surfaceVariant,
    )
}

private fun SettingsCategory.icon(): ImageVector = when (this) {
    SettingsCategory.GENERAL    -> Icons.Outlined.Tune
    SettingsCategory.PLAYER     -> Icons.Outlined.PlayCircle
    SettingsCategory.PROVIDERS  -> Icons.Outlined.Storage
    SettingsCategory.UI         -> Icons.Outlined.Palette
    SettingsCategory.UPDATES    -> Icons.Outlined.SystemUpdate
    SettingsCategory.ACCOUNT    -> Icons.Outlined.AccountCircle
    SettingsCategory.EXTENSIONS -> Icons.Outlined.Extension
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsVersionFooter(version: SettingsVersionState, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = {})
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
        color = CloudStreamTheme.colors.onBackground,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun VersionDot() {
    Text(
        text = "•",
        color = CloudStreamTheme.colors.onBackground,
        fontSize = 12.sp,
    )
}
