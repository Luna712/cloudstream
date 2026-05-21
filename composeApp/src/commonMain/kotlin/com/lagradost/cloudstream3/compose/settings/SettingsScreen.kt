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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.compose.generated.resources.Res
import com.lagradost.cloudstream3.compose.generated.resources.category_account
import com.lagradost.cloudstream3.compose.generated.resources.category_extensions
import com.lagradost.cloudstream3.compose.generated.resources.category_general
import com.lagradost.cloudstream3.compose.generated.resources.category_player
import com.lagradost.cloudstream3.compose.generated.resources.category_providers
import com.lagradost.cloudstream3.compose.generated.resources.category_ui
import com.lagradost.cloudstream3.compose.generated.resources.category_updates
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_blue
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_dark_blue
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_orange
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_pink
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_purple
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_red
import com.lagradost.cloudstream3.compose.generated.resources.profile_bg_teal
import com.lagradost.cloudstream3.compose.icons.account_circle
import com.lagradost.cloudstream3.compose.icons.extension
import com.lagradost.cloudstream3.compose.icons.mobile_arrow_down
import com.lagradost.cloudstream3.compose.icons.palette
import com.lagradost.cloudstream3.compose.icons.play_circle
import com.lagradost.cloudstream3.compose.icons.storage
import com.lagradost.cloudstream3.compose.icons.tune
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
        SettingsCategory.ACCOUNT    -> Res.string.category_accounts
        SettingsCategory.EXTENSIONS -> Res.string.category_extensions
    }
)

private fun SettingsCategory.icon(): ImageVector = when (this) {
    SettingsCategory.GENERAL    -> tune
    SettingsCategory.PLAYER     -> play_circle
    SettingsCategory.PROVIDERS  -> storage
    SettingsCategory.UI         -> palette
    SettingsCategory.UPDATES    -> mobile_arrow_down
    SettingsCategory.ACCOUNT    -> account_circle
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsProfileHeader(profile = profile, avatarContent = avatarContent)

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
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
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

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = profile.name,
            color = colors.onBackground,
            style = MaterialTheme.typography.titleMedium,
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = label,
            color = colors.onBackground,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
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
        color = CloudStreamTheme.colors.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun VersionDot() {
    Text(
        text = "•",
        color = CloudStreamTheme.colors.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}
