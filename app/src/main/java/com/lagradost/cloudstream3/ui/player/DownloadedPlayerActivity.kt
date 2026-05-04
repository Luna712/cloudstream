package com.lagradost.cloudstream3.ui.player

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.ui.player.OfflinePlaybackHelper.playLink
import com.lagradost.cloudstream3.ui.player.OfflinePlaybackHelper.playUri
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.attachBackPressedCallback
import com.lagradost.cloudstream3.utils.DataStore.getSharedPrefs
import com.lagradost.cloudstream3.utils.UIHelper.enableEdgeToEdgeCompat

class DownloadedPlayerActivity : AppCompatActivity() {
    companion object {
        const val TAG = "DownloadedPlayerActivity"

        /**
         * SharedPreferences key for the most recent intent delivered to this Activity.
         *
         * Android's singleTask launch mode uses the **task base intent** (the intent that
         * originally created the task) when relaunching from recents after a process kill.
         * Intents delivered via [onNewIntent] update [getIntent] on the current instance only —
         * they are NOT reflected in the task base intent. If the process is killed while a
         * newer file is playing (before [onSaveInstanceState] had a chance to run), Android
         * restores using the stale base intent and the wrong file plays.
         *
         * Fix: persist the latest intent as a Parcel byte array in SharedPreferences on every
         * [onNewIntent] delivery. On cold [onCreate] (savedInstanceState == null, meaning no
         * system-managed state to restore from), substitute the persisted intent so the correct
         * file is always opened regardless of what the task base intent says.
         */
        private const val PREF_LAST_INTENT = "downloaded_player_last_intent"
    }

    private fun saveIntentToPrefs(intent: Intent) {
        safe {
            val settingsManager = context?.getSharedPrefs() ?: return@safe
            val parcel = Parcel.obtain()
            try {
                intent.writeToParcel(parcel, 0)
                val bytes = parcel.marshall()
                settingsManager.edit {
                    putString(PREF_LAST_INTENT, Base64.encodeToString(bytes, Base64.DEFAULT))
                }
            } finally {
                parcel.recycle()
            }
        }
    }

    private fun loadIntentFromPrefs(): Intent? = safe {
        val prefs = context?.getSharedPrefs()
        val b64 = prefs?.getString(PREF_LAST_INTENT, null) ?: return@safe null
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            Intent.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        CommonActivity.dispatchKeyEvent(this, event) ?: super.dispatchKeyEvent(event)

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
        CommonActivity.onKeyDown(this, keyCode, event) ?: super.onKeyDown(keyCode, event)

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        CommonActivity.onUserLeaveHint(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Ignore same intent so the player doesn't totally
        // reload if you are playing the same thing.
        if (isSameIntent(intent)) return
        setIntent(intent)
        // Persist the new intent immediately so it survives a system process kill.
        // The task base intent is only updated when the Activity is freshly started at the
        // root of the task — onNewIntent deliveries don't update it. Without this, a
        // subsequent kill+relaunch from recents would replay the original (stale) intent.
        saveIntentToPrefs(intent)
        Log.i(TAG, "onNewIntent")
        handleIntent(intent)
    }

    private fun isSameIntent(newIntent: Intent): Boolean {
        val old = intent ?: return false
        // Compare URIs first
        val oldUri = old.data ?: old.clipData?.getItemAt(0)?.uri
        val newUri = newIntent.data ?: newIntent.clipData?.getItemAt(0)?.uri
        if (oldUri != null && oldUri == newUri) return true
        // Fall back to comparing EXTRA_TEXT links
        val oldText = safe { old.getStringExtra(Intent.EXTRA_TEXT) }
        val newText = safe { newIntent.getStringExtra(Intent.EXTRA_TEXT) }
        return oldText != null && oldText == newText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CommonActivity.loadThemes(this)
        CommonActivity.init(this)
        enableEdgeToEdgeCompat()
        setContentView(R.layout.empty_layout)
        Log.i(TAG, "onCreate")

        // When savedInstanceState != null the system is restoring a backgrounded Activity
        // whose nav back stack is already rebuilt by the NavController — no need to replay
        // the intent at all (the correct player fragment is already in the stack).
        //
        // When savedInstanceState == null this is a cold start: either the first launch or a
        // relaunch from recents after a process kill with no saved state. In the kill case the
        // system uses the task base intent (the ORIGINAL launch intent), which may be stale if
        // newer files were opened via onNewIntent. Substitute the persisted latest intent so
        // the correct file is always played.
        if (savedInstanceState == null) {
            val latestIntent = loadIntentFromPrefs() ?: intent
            // Update getIntent() so isSameIntent() comparisons stay consistent.
            setIntent(latestIntent)
            handleIntent(latestIntent)
        }

        attachBackPressedCallback("DownloadedPlayerActivity") { finish() }
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (OfflinePlaybackHelper.playIntent(activity = this, intent = intent)) {
            return
        }

        if (
            intent.action == Intent.ACTION_SEND ||
            intent.action == Intent.ACTION_OPEN_DOCUMENT ||
            intent.action == Intent.ACTION_VIEW
        ) {
            val extraText = safe { intent.getStringExtra(Intent.EXTRA_TEXT) }
            val cd = intent.clipData
            val item = if (cd != null && cd.itemCount > 0) cd.getItemAt(0) else null
            val url = item?.text?.toString()
            when {
                item?.uri != null -> playUri(this, item.uri)
                url != null -> playLink(this, url)
                data != null -> playUri(this, data)
                extraText != null -> playLink(this, extraText)
                else -> finish()
            }
        } else if (data?.scheme == "content") {
            playUri(this, data)
        } else finish()
    }

    override fun onResume() {
        super.onResume()
        CommonActivity.setActivityInstance(this)
    }
}
