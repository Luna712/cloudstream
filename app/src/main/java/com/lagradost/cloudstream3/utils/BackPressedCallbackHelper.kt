package com.lagradost.cloudstream3.utils

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.util.WeakHashMap

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

    private class Dispatcher {
        private val enabledChangedListeners = mutableListOf<(Boolean) -> Unit>()
        var isEnabled: Boolean = true
            set(value) {
                field = value
                enabledChangedListeners.forEach { it(value) }
            }

        fun addEnabledChangedListener(listener: (Boolean) -> Unit) {
            enabledChangedListeners += listener
        }

        fun startPredictiveBack(event: BackPressedCallbackHelper.BackEvent) {}
        fun progressPredictiveBack(event: BackPressedCallbackHelper.BackEvent) {}
        fun cancelPredictiveBack() {}
        fun back() {}
    }

    data class BackEvent(
        val progress: Float,
        val swipeEdge: SwipeEdge,
        val touchX: Float,
        val touchY: Float
    ) {
        enum class SwipeEdge { LEFT, RIGHT, UNKNOWN }
    }

    fun ComponentActivity.attachBackPressedCallback(
        id: String,
        callback: CallbackHelper.() -> Unit
    ) {
        val callbackMap = backPressedCallbacks.getOrPut(this) { mutableMapOf() }
        if (callbackMap.containsKey(id)) return

        val dispatcher = Dispatcher()

        val cb = object : OnBackPressedCallback(dispatcher.isEnabled) {

            init {
                dispatcher.addEnabledChangedListener { isEnabled = it }
            }

            override fun handleOnBackPressed() {
                callback(CallbackHelper(this@attachBackPressedCallback, this))
            }

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                dispatcher.startPredictiveBack(backEvent.toBackEvent())
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                dispatcher.progressPredictiveBack(backEvent.toBackEvent())
            }

            override fun handleOnBackCancelled() {
                dispatcher.cancelPredictiveBack()
            }

            private fun BackEventCompat.toBackEvent(): BackEvent =
                BackEvent(
                    progress = progress,
                    swipeEdge = when (swipeEdge) {
                        BackEventCompat.EDGE_LEFT -> BackEvent.SwipeEdge.LEFT
                        BackEventCompat.EDGE_RIGHT -> BackEvent.SwipeEdge.RIGHT
                        else -> BackEvent.SwipeEdge.UNKNOWN
                    },
                    touchX = touchX,
                    touchY = touchY
                )
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

