package com.lagradost.cloudstream3.compose.settings

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
import com.lagradost.cloudstream3.compose.theme.CloudStreamTheme
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
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
private fun SettingsCategoryRow(label: String, onClick: () -> Unit) {
    val colors = CloudStreamTheme.colors
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        color = colors.onBackground,
        fontSize = 16.sp,
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = colors.surfaceVariant,
    )
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
