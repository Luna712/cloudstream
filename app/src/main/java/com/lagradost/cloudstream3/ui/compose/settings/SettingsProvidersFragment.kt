package com.lagradost.cloudstream3.ui.compose.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AllLanguagesName
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.shared.ui.settings.providers.SettingsProvidersScreen
import com.lagradost.cloudstream3.shared.ui.settings.providers.SettingsProvidersState
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme
import com.lagradost.cloudstream3.shared.ui.theme.loadPrimaryColor
import com.lagradost.cloudstream3.shared.ui.theme.loadThemeMode
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiDubstatusSettings
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiProviderLangSettings
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.SubtitleHelper.getNameNextToFlagEmoji

class SettingsProvidersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        setContent {
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)

            // Build initial state from preferences
            val dubList = DubStatus.entries
            val tvTypes = enumValues<TvType>().sorted()

            val currentDubStatuses = activity?.getApiDubstatusSettings()
                ?.map { dubList.indexOf(it) }
                ?.filter { it >= 0 }
                ?: dubList.indices.toList()

            val defaultMediaSet = tvTypes
                .filter { it != TvType.NSFW }
                .map { it.ordinal.toString() }
                .toSet()
            val currentMediaTypes = try {
                settingsManager.getStringSet(
                    getString(R.string.prefer_media_type_key), defaultMediaSet
                )?.mapNotNull { it.toIntOrNull() }
                    ?.map { ordinal -> tvTypes.indexOfFirst { it.ordinal == ordinal } }
                    ?.filter { it >= 0 }
            } catch (e: Throwable) { null }
                ?: tvTypes.indices.filter { tvTypes[it] != TvType.NSFW }

            val languagesTagName = synchronized(APIHolder.apis) {
                listOf(Pair(AllLanguagesName, getString(R.string.all_languages_preference))) +
                    APIHolder.apis
                        .map { Pair(it.lang, getNameNextToFlagEmoji(it.lang) ?: it.lang) }
                        .toSet()
                        .sortedBy { it.second.substringAfter("\u00a0").lowercase() }
            }

            val currentLangTags = activity?.getApiProviderLangSettings() ?: hashSetOf(AllLanguagesName)
            val currentLangIndices = currentLangTags.map { langTag ->
                languagesTagName.indexOfFirst { it.first == langTag }
            }.filter { it >= 0 }

            var state by remember {
                mutableStateOf(
                    SettingsProvidersState(
                        nsfwEnabled = settingsManager.getBoolean(
                            getString(R.string.enable_nsfw_on_providers_key), false
                        ),
                        selectedDubStatuses = currentDubStatuses,
                        selectedMediaTypes = currentMediaTypes,
                        selectedLanguageIndices = currentLangIndices,
                    )
                )
            }

            CloudStreamTheme(
                mode = context.loadThemeMode(),
                primaryColor = context.loadPrimaryColor(),
            ) {
                SettingsProvidersScreen(
                    state = state,
                    onBack = { activity?.onBackPressedDispatcher?.onBackPressed() },
                    onNsfwToggle = { enabled ->
                        settingsManager.edit {
                            putBoolean(getString(R.string.enable_nsfw_on_providers_key), enabled)
                        }
                        state = state.copy(nsfwEnabled = enabled)
                    },
                    onDubStatusChanged = { selectedIndices ->
                        val selectedStatuses = selectedIndices.map { dubList[it] }
                        APIRepository.dubStatusActive = selectedStatuses.toHashSet()
                        settingsManager.edit {
                            putStringSet(
                                getString(R.string.display_sub_key),
                                selectedStatuses.map { it.name }.toMutableSet()
                            )
                        }
                        state = state.copy(selectedDubStatuses = selectedIndices)
                    },
                    onMediaTypesChanged = { selectedIndices ->
                        settingsManager.edit {
                            putStringSet(
                                getString(R.string.prefer_media_type_key),
                                selectedIndices.map { tvTypes[it].ordinal.toString() }.toMutableSet()
                            )
                        }
                        DataStoreHelper.currentHomePage = null
                        state = state.copy(selectedMediaTypes = selectedIndices)
                    },
                    onLanguagesChanged = { selectedIndices ->
                        settingsManager.edit {
                            putStringSet(
                                getString(R.string.provider_lang_key),
                                selectedIndices.map { languagesTagName[it].first }.toSet()
                            )
                        }
                        state = state.copy(selectedLanguageIndices = selectedIndices)
                    },
                    onTestExtensionsClick = {
                        val options = NavOptions.Builder()
                            .setEnterAnim(R.anim.enter_anim)
                            .setExitAnim(R.anim.exit_anim)
                            .setPopEnterAnim(R.anim.pop_enter)
                            .setPopExitAnim(R.anim.pop_exit)
                            .build()
                        findNavController().navigate(
                            R.id.navigation_test_providers, null, options
                        )
                    },
                )
            }
        }
    }
}
