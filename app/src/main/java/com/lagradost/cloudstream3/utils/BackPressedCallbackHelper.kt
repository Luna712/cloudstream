package com.lagradost.cloudstream3.utils

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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

    private class Dispatcher(private val targetView: View) {
        var predictiveActive = false

        fun startPredictiveBack(event: BackEvent) {
            predictiveActive = true
            targetView.translationX = 0f
            targetView.alpha = 1f
        }

        fun progressPredictiveBack(event: BackEvent) {
            val progress = min(1f, max(0f, event.progress))
            targetView.translationX = targetView.width * progress
            targetView.alpha = 1f - progress * 0.5f
        }

        fun cancelPredictiveBack() {
            predictiveActive = false
            targetView.animate().translationX(0f).alpha(1f).setDuration(200).start()
        }

        fun toBackEvent(backEventCompat: BackEventCompat): BackEvent =
            BackEvent(
                progress = backEventCompat.progress,
                swipeEdge = when (backEventCompat.swipeEdge) {
                    BackEventCompat.EDGE_LEFT -> BackEvent.SwipeEdge.LEFT
                    BackEventCompat.EDGE_RIGHT -> BackEvent.SwipeEdge.RIGHT
                    else -> BackEvent.SwipeEdge.UNKNOWN
                },
                touchX = backEventCompat.touchX,
                touchY = backEventCompat.touchY
            )
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
        rootView: View? = null,
        callback: CallbackHelper.() -> Unit
    ) {
        val callbackMap = backPressedCallbacks.getOrPut(this) { mutableMapOf() }
        if (callbackMap.containsKey(id)) return

        val targetView = rootView ?: window.decorView
        val dispatcher = Dispatcher(targetView)

        val cb = object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                dispatcher.startPredictiveBack(dispatcher.toBackEvent(backEvent))
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                dispatcher.progressPredictiveBack(dispatcher.toBackEvent(backEvent))
            }

            override fun handleOnBackCancelled() {
                dispatcher.cancelPredictiveBack()
            }

            override fun handleOnBackPressed() {
                callback(CallbackHelper(this@attachBackPressedCallback, this))
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
