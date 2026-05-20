package com.lagradost.cloudstream3.compose.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.compose.theme.CloudStreamTheme
import com.lagradost.cloudstream3.compose.theme.loadCloudStreamThemeMode
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.GitInfo.currentCommitHash
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.txt
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// TODO: Move to composeApp once we have shared logic available in the
// :core module and we have migrated to navigation3 (which we can not
// do until all fragments have migrated to compose).
class SettingsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        setContent {
            CloudStreamTheme(mode = context.loadCloudStreamThemeMode()) {
                SettingsScreen(
                    profile = buildProfileState(),
                    version = buildVersionState(),
                    avatarContent = { profilePic ->
                        AsyncImage(
                            model = profilePic,
                            contentDescription = getString(R.string.account),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    onNavigate = ::navigateTo,
                    onVersionLongClick = {
                        val v = BuildConfig.VERSION_NAME
                        val h = activity?.currentCommitHash() ?: ""
                        val d = buildVersionState().buildDate
                        clipboardHelper(txt(R.string.extension_version), "$v $h $d")
                    },
                )
            }
        }
    }

    private fun buildProfileState(): SettingsProfileState {
        for (syncApi in AccountManager.allApis) {
            val login = syncApi.authUser() ?: continue
            return SettingsProfileState(name = login.name, profilePictureUrl = login.profilePicture)
        }
        val account = runCatching {
            DataStoreHelper.accounts.firstOrNull {
                it.keyIndex == DataStoreHelper.selectedKeyIndex
            } ?: DataStoreHelper.getDefaultAccount(requireActivity())
        }.getOrNull()

        return SettingsProfileState(name = account?.name ?: "", profilePictureUrl = account?.image)
    }

    private fun buildVersionState(): SettingsVersionState {
        val buildDate = SimpleDateFormat
            .getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(BuildConfig.BUILD_DATE))
            .replace("UTC", "")

        return SettingsVersionState(
            appVersion = BuildConfig.VERSION_NAME,
            commitHash = activity?.currentCommitHash() ?: "",
            buildDate = buildDate,
        )
    }

    private fun navigateTo(category: SettingsCategory) {
        val actionId = when (category) {
            SettingsCategory.GENERAL    -> R.id.action_navigation_global_to_navigation_settings_general
            SettingsCategory.PLAYER     -> R.id.action_navigation_global_to_navigation_settings_player
            SettingsCategory.PROVIDERS  -> R.id.action_navigation_global_to_navigation_settings_providers
            SettingsCategory.UI         -> R.id.action_navigation_global_to_navigation_settings_ui
            SettingsCategory.UPDATES    -> R.id.action_navigation_global_to_navigation_settings_updates
            SettingsCategory.ACCOUNT    -> R.id.action_navigation_global_to_navigation_settings_account
            SettingsCategory.EXTENSIONS -> R.id.action_navigation_global_to_navigation_settings_extensions
        }
        activity?.navigate(actionId, Bundle())
    }
}

@Suppress("FunctionName")
private fun SettingsScreen(
    profile: SettingsProfileState,
    version: SettingsVersionState,
    avatarContent: @Composable (profilePicUrl: String) -> Unit,
    onNavigate: (SettingsCategory) -> Unit,
    onVersionLongClick: () -> Unit,
) = SettingsScreen(
    profile = profile,
    version = version,
    avatarContent = { profile.profilePictureUrl?.let { avatarContent(it) } ?: Unit },
    onNavigate = onNavigate,
    onVersionLongClick = onVersionLongClick,
)
