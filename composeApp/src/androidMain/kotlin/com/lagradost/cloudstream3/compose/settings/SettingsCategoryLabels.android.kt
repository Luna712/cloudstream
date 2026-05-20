package com.lagradost.cloudstream3.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.lagradost.cloudstream3.R

@Composable
actual fun SettingsCategory.label(): String {
    val ctx = LocalContext.current
    return ctx.getString(
        when (this) {
            SettingsCategory.GENERAL    -> R.string.category_general
            SettingsCategory.PLAYER     -> R.string.category_player
            SettingsCategory.PROVIDERS  -> R.string.category_providers
            SettingsCategory.UI         -> R.string.category_ui
            SettingsCategory.UPDATES    -> R.string.category_updates
            SettingsCategory.ACCOUNT    -> R.string.category_account
            SettingsCategory.EXTENSIONS -> R.string.extensions
        }
    )
}
