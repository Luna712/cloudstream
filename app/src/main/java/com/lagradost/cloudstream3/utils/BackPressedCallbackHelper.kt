package com.lagradost.cloudstream3.utils

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.BackEventCompat
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min

object BackPressedCallbackHelper {

    private val backPressedCallbacks =
        WeakHashMap<ComponentActivity, MutableMap<String, OnBackPressedCallback>>()

    class CallbackHelper(
        private val activity: ComponentActivity,
        private val callback: OnBackPressedCallback
    ) {
        fun runDefault() {
            val wasEnabled = callback.isEnabled
            callback.isEnabled = false
            try {
                activity.onBackPressedDispatcher.onBackPressed()
            } finally {
                callback.isEnabled = wasEnabled
            }
        }
    }

    fun ComponentActivity.attachBackPressedCallback(
        id: String,
        rootView: View? = null,
        callback: CallbackHelper.() -> Unit
    ) {
        val callbackMap = backPressedCallbacks.getOrPut(this) { mutableMapOf() }
        if (callbackMap.containsKey(id)) return

        val targetView = rootView ?: window.decorView

        val cb = object : OnBackPressedCallback(true) {
            private var predictiveActive = false

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                predictiveActive = true
                targetView.translationX = 0f
                targetView.alpha = 1f
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (!predictiveActive) predictiveActive = true
                val progress = min(1f, max(0f, backEvent?.progress ?: 0f))
                targetView.translationX = targetView.width * progress
                targetView.alpha = 1f - progress * 0.5f
            }

            override fun handleOnBackCancelled() {
                if (!predictiveActive) return
                predictiveActive = false
                targetView.animate().translationX(0f).alpha(1f).setDuration(200).start()
            }

            override fun handleOnBackPressed() {
                if (!predictiveActive) return
                predictiveActive = false
                targetView.animate().translationX(targetView.width.toFloat())
                    .alpha(0f).setDuration(200).withEndAction {
                        CallbackHelper(this@attachBackPressedCallback, this).callback()
                    }.start()
            }
        }

        callbackMap[id] = cb
        onBackPressedDispatcher.addCallback(this, cb)
    }

    fun ComponentActivity.disableBackPressedCallback(id: String) {
        backPressedCallbacks[this]?.get(id)?.isEnabled = false
    }

    fun ComponentActivity.enableBackPressedCallback(id: String) {
        backPressedCallbacks[this]?.get(id)?.isEnabled = true
    }

    fun ComponentActivity.detachBackPressedCallback(id: String) {
        val callbackMap = backPressedCallbacks[this] ?: return
        callbackMap[id]?.let { callback ->
            callback.isEnabled = false
            callbackMap.remove(id)
        }
        if (callbackMap.isEmpty()) backPressedCallbacks.remove(this)
    }
}
