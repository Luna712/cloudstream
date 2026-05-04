package com.lagradost.cloudstream3.ui.player

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavOptions
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.actions.temp.CloudStreamPackage
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.safefile.SafeFile

object OfflinePlaybackHelper {

    /**
     * NavOptions that pop any existing player off the back stack before pushing the new one.
     *
     * Without this, every [android.app.Activity.onNewIntent] call in [DownloadedPlayerActivity]
     * stacks another GeneratorPlayer on top of the previous one:
     *   start → player(file1) → player(file2) → player(file3) …
     *
     * This has two consequences:
     *  1. Android only persists the nav back-stack in [Activity.onSaveInstanceState], which runs
     *     asynchronously after [onNewIntent]. If the process is killed before that runs, the
     *     saved state is stale — restoring the wrong (older) player fragment.
     *  2. Pressing back cycles through every previously-opened file rather than leaving the player.
     *
     * By popping up to (and including) the player destination before each navigate() call,
     * the stack stays flat:  start → player(fileN)
     * The latest generator is always the only one, so save/restore is always correct.
     */
    private val replacePlayerNavOptions = NavOptions.Builder()
        .setPopUpTo(R.id.navigation_player, inclusive = true, saveState = false)
        .build()

    fun playLink(activity: Activity, url: String) {
        activity.navigate(
            R.id.global_to_navigation_player, GeneratorPlayer.newInstance(
                LinkGenerator(
                    listOf(
                        BasicLink(url)
                    )
                )
            ),
            replacePlayerNavOptions
        )
    }

    // See CloudStreamPackage
    fun playIntent(activity: Activity, intent: Intent?): Boolean {
        if (intent == null) return false
        val links = intent.getStringArrayExtra(CloudStreamPackage.LINKS_EXTRA)
            ?.mapNotNull { tryParseJson<CloudStreamPackage.MinimalVideoLink>(it) } ?: emptyList()
        if (links.isEmpty()) return false
        val subs = intent.getStringArrayExtra(CloudStreamPackage.SUBTITLE_EXTRA)
            ?.mapNotNull { tryParseJson<CloudStreamPackage.MinimalSubtitleLink>(it) } ?: emptyList()

        val id = intent.getIntExtra(CloudStreamPackage.ID_EXTRA, -1)
        //val title = intent.getStringExtra(CloudStreamPackage.TITLE_EXTRA) // unused
        val pos = intent.getLongExtra(CloudStreamPackage.POSITION_EXTRA, -1L)
        val dur = intent.getLongExtra(CloudStreamPackage.DURATION_EXTRA, -1L)

        if (id != -1 && pos != -1L) {
            val duration = if (dur != -1L) {
                dur
            } else DataStoreHelper.getViewPos(id)?.duration ?: pos
            DataStoreHelper.setViewPos(id, pos, duration)
        }

        activity.navigate(
            R.id.global_to_navigation_player, GeneratorPlayer.newInstance(
                MinimalLinkGenerator(
                    links,
                    subs,
                    if (id != -1) id else null,
                )
            ),
            replacePlayerNavOptions
        )
        return true
    }

    fun playUri(activity: Activity, uri: Uri) {
        if (uri.scheme == "magnet") {
            playLink(activity, uri.toString())
            return
        }
        val name = SafeFile.fromUri(activity, uri)?.name()
        activity.navigate(
            R.id.global_to_navigation_player, GeneratorPlayer.newInstance(
                DownloadFileGenerator(
                    listOf(
                        ExtractorUri(
                            uri = uri,
                            name = name ?: getString(activity, R.string.downloaded_file),
                            // well not the same as a normal id, but we take it as users may want to
                            // play downloaded files and save the location
                            id = kotlin.runCatching { ContentUris.parseId(uri) }.getOrNull()
                                ?.hashCode()
                        )
                    )
                )
            ),
            replacePlayerNavOptions
        )
    }
}
