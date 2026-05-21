package com.lagradost.cloudstream3.shared.ui.settings.providers

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import com.lagradost.cloudstream3.AllLanguagesName
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.shared.generated.resources.Res
import com.lagradost.cloudstream3.shared.generated.resources.all_languages_preference
import com.lagradost.cloudstream3.shared.generated.resources.display_subbed_dubbed_settings
import com.lagradost.cloudstream3.shared.generated.resources.enable_nsfw_on_providers
import com.lagradost.cloudstream3.shared.generated.resources.preferred_media_settings
import com.lagradost.cloudstream3.shared.generated.resources.provider_lang_settings
import com.lagradost.cloudstream3.shared.generated.resources.settings_providers_title
import com.lagradost.cloudstream3.shared.generated.resources.test_extensions
import com.lagradost.cloudstream3.shared.generated.resources.test_extensions_summary
import com.lagradost.cloudstream3.shared.ui.components.settings.MultiSelectDialog
import com.lagradost.cloudstream3.shared.ui.components.settings.SettingsItem
import com.lagradost.cloudstream3.shared.ui.components.settings.SettingsScaffold
import com.lagradost.cloudstream3.shared.ui.components.settings.SettingsSwitch
import com.lagradost.cloudstream3.shared.ui.icons.extension
import com.lagradost.cloudstream3.shared.ui.icons.language
import com.lagradost.cloudstream3.shared.ui.icons.network_ping
import com.lagradost.cloudstream3.shared.ui.icons.play_circle
import com.lagradost.cloudstream3.shared.ui.icons.voice_over_off
import com.lagradost.cloudstream3.utils.SubtitleHelper.getNameNextToFlagEmoji
import org.jetbrains.compose.resources.stringResource

data class SettingsProvidersState(
    val nsfwEnabled: Boolean,
    val selectedDubStatuses: List<Int>,
    val selectedMediaTypes: List<Int>,
    val selectedLanguageIndices: List<Int>,
)

private enum class ProvidersDialog {
    NONE, LANGUAGE, MEDIA_TYPE, SUB_DUB
}

@Composable
fun SettingsProvidersScreen(
    state: SettingsProvidersState,
    onBack: () -> Unit,
    onNsfwToggle: (Boolean) -> Unit,
    onDubStatusChanged: (List<Int>) -> Unit,
    onMediaTypesChanged: (List<Int>) -> Unit,
    onLanguagesChanged: (List<Int>) -> Unit,
    onTestExtensionsClick: () -> Unit,
) {
    var openDialog by remember { mutableStateOf(ProvidersDialog.NONE) }

    val dubList = DubStatus.entries
    val tvTypes = enumValues<TvType>().sorted()

    val languagesTagName = remember {
        synchronized(APIHolder.apis) {
            listOf(Pair(AllLanguagesName, AllLanguagesName)) +
                APIHolder.apis
                    .map { Pair(it.lang, getNameNextToFlagEmoji(it.lang) ?: it.lang) }
                    .toSet()
                    .sortedBy { it.second.substringAfter("\u00a0").lowercase() }
        }
    }

    // Dialogs
    when (openDialog) {
        ProvidersDialog.SUB_DUB -> MultiSelectDialog(
            title = stringResource(Res.string.display_subbed_dubbed_settings),
            items = dubList.map { it.name },
            selectedIndices = state.selectedDubStatuses,
            onDismiss = { openDialog = ProvidersDialog.NONE },
            onConfirm = {
                onDubStatusChanged(it)
                openDialog = ProvidersDialog.NONE
            },
        )
        ProvidersDialog.MEDIA_TYPE -> MultiSelectDialog(
            title = stringResource(Res.string.preferred_media_settings),
            items = tvTypes.map { it.name },
            selectedIndices = state.selectedMediaTypes,
            onDismiss = { openDialog = ProvidersDialog.NONE },
            onConfirm = {
                onMediaTypesChanged(it)
                openDialog = ProvidersDialog.NONE
            },
        )
        ProvidersDialog.LANGUAGE -> MultiSelectDialog(
            title = stringResource(Res.string.provider_lang_settings),
            items = languagesTagName.map { it.second },
            selectedIndices = state.selectedLanguageIndices,
            onDismiss = { openDialog = ProvidersDialog.NONE },
            onConfirm = {
                onLanguagesChanged(it)
                openDialog = ProvidersDialog.NONE
            },
        )
        ProvidersDialog.NONE -> Unit
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_providers_title),
        onBack = onBack,
    ) {
        item {
            SettingsItem(
                title = stringResource(Res.string.provider_lang_settings),
                icon = language,
                onClick = { openDialog = ProvidersDialog.LANGUAGE },
            )
        }
        item {
            SettingsItem(
                title = stringResource(Res.string.preferred_media_settings),
                icon = play_circle,
                onClick = { openDialog = ProvidersDialog.MEDIA_TYPE },
            )
        }
        item {
            SettingsItem(
                title = stringResource(Res.string.display_subbed_dubbed_settings),
                icon = record_voice_over_off,
                onClick = { openDialog = ProvidersDialog.SUB_DUB },
            )
        }
        item {
            SettingsSwitch(
                title = stringResource(Res.string.enable_nsfw_on_providers),
                checked = state.nsfwEnabled,
                onCheckedChange = onNsfwToggle,
                icon = extension,
            )
        }
        item {
            SettingsItem(
                title = stringResource(Res.string.test_extensions),
                subtitle = stringResource(Res.string.test_extensions_summary),
                icon = network_ping,
                onClick = onTestExtensionsClick,
            )
        }
    }
}
