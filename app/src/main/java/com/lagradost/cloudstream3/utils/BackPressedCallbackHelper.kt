package com.lagradost.cloudstream3.utils

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.util.WeakHashMap

object BackPressedCallbackHelper {

    private val callbacks =
        WeakHashMap<ComponentActivity, MutableMap<String, Any>>() // holds *either* callback type

    class CallbackHelper(
        private val activity: ComponentActivity,
        private val backPressed: OnBackPressedCallback?
    ) {
        fun runDefault() {
            if (backPressed != null) {
                val wasEnabled = backPressed.isEnabled
                backPressed.isEnabled = false
                try {
                    activity.onBackPressedDispatcher.onBackPressed()
                } finally {
                    backPressed.isEnabled = wasEnabled
                }
            }
            // For 33+, the system automatically runs default behavior
            // once our callback doesn't consume the back gesture.
        }
    }

    fun ComponentActivity.attachBackPressedCallback(
        id: String,
        callback: CallbackHelper.() -> Unit
    ) {
        val map = callbacks.getOrPut(this) { mutableMapOf() }
        if (map.containsKey(id)) return

        if (Build.VERSION.SDK_INT >= 33) {
            val cb = OnBackInvokedCallback {
                CallbackHelper(this, null).callback()
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                cb
            )
            map[id] = cb
        } else {
            val cb = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CallbackHelper(this@attachBackPressedCallback, this).callback()
                }
            }
            onBackPressedDispatcher.addCallback(this, cb)
            map[id] = cb
        }
    }

    fun ComponentActivity.disableBackPressedCallback(id: String) {
        val cb = callbacks[this]?.get(id) ?: return
        if (cb is OnBackPressedCallback) {
            cb.isEnabled = false
        }
    }

    fun ComponentActivity.enableBackPressedCallback(id: String) {
        val cb = callbacks[this]?.get(id) ?: return
        if (cb is OnBackPressedCallback) {
            cb.isEnabled = true
        }
    }

    fun ComponentActivity.detachBackPressedCallback(id: String) {
        val map = callbacks[this] ?: return
        val cb = map[id] ?: return

        if (Build.VERSION.SDK_INT >= 33 && cb is OnBackInvokedCallback) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(cb)
        } else if (cb is OnBackPressedCallback) {
            cb.isEnabled = false
        }

        map.remove(id)
        if (map.isEmpty()) {
            callbacks.remove(this)
        }
    }
}
