package com.lagradost.cloudstream3.utils

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.activity.OnBackPressedDispatcher
import java.util.WeakHashMap
import android.window.OnBackInvokedDispatcher
import android.window.OnBackInvokedCallback

object BackPressedCallbackHelper {
    private val backPressedCallbacks =
        WeakHashMap<ComponentActivity, MutableMap<String, Any>>()

    /**
     * Attaches a back press callback that works for both legacy and predictive back.
     */
    fun ComponentActivity.attachBackPressedCallback(id: String, callback: () -> Unit) {
        val callbackMap = backPressedCallbacks.getOrPut(this) { mutableMapOf() }

        // Avoid duplicate registration
        if (callbackMap.containsKey(id)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ predictive back system
            val predictiveCallback = OnBackInvokedCallback {
                callback.invoke()
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_SYSTEM_NAVIGATION_OBSERVER,
                predictiveCallback
            )
            callbackMap[id] = predictiveCallback
        } else {
            // Legacy back press
            val legacyCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    callback.invoke()
                }
            }
            onBackPressedDispatcher.addCallback(this, legacyCallback)
            callbackMap[id] = legacyCallback
        }
    }

    /**
     * Disable callback (works for both predictive and legacy)
     */
    fun ComponentActivity.disableBackPressedCallback(id: String) {
        val map = backPressedCallbacks[this] ?: return
        when (val callback = map[id]) {
            is OnBackPressedCallback -> callback.isEnabled = false
            // For predictive back, no enable/disable toggle - so just ignore
        }
    }

    /**
     * Enable callback (works for both predictive and legacy)
     */
    fun ComponentActivity.enableBackPressedCallback(id: String) {
        val map = backPressedCallbacks[this] ?: return
        when (val callback = map[id]) {
            is OnBackPressedCallback -> callback.isEnabled = true
        }
    }

    /**
     * Detach callback safely
     */
    fun ComponentActivity.detachBackPressedCallback(id: String) {
        val map = backPressedCallbacks[this] ?: return
        when (val callback = map.remove(id)) {
            is OnBackPressedCallback -> {
                callback.isEnabled = false
            }
            is OnBackInvokedCallback -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
                }
            }
        }
        if (map.isEmpty()) backPressedCallbacks.remove(this)
    }

}
