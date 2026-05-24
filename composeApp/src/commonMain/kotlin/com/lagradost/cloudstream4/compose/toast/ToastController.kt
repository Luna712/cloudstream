package com.lagradost.cloudstream4.compose.toast

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

enum class ToastType { Info, Success, Warning, Error }

data class ToastEvent(
    val message: String,
    val type: ToastType = ToastType.Info,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val dismissable: Boolean = false,
    val queue: Boolean = false,
)

object ToastController {
    private val _events = Channel<ToastEvent>(Channel.BUFFERED)
    internal val events = _events.receiveAsFlow()

    internal fun drain(): ToastEvent? = _events.tryReceive().getOrNull()

    fun post(
        message: String,
        type: ToastType = ToastType.Info,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) {
        if (!queue) _events.tryReceive()
        _events.trySend(ToastEvent(message, type, duration, actionLabel, onAction, dismissable, queue))
    }

    fun postSuccess(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) = post(message, ToastType.Success, duration, dismissable = dismissable, queue = queue)

    fun postWarning(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) = post(message, ToastType.Warning, duration, dismissable = dismissable, queue = queue)

    fun postError(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) = post(message, ToastType.Error, duration, dismissable = dismissable, queue = queue)

    suspend fun show(
        message: String,
        type: ToastType = ToastType.Info,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) {
        if (!queue) _events.tryReceive()
        _events.send(ToastEvent(message, type, duration, actionLabel, onAction, dismissable, queue))
    }

    suspend fun showSuccess(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) = show(message, ToastType.Success, duration, dismissable = dismissable, queue = queue)

    suspend fun showWarning(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) = show(message, ToastType.Warning, duration, dismissable = dismissable, queue = queue)

    suspend fun showError(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        dismissable: Boolean = false,
        queue: Boolean = false,
    ) = show(message, ToastType.Error, duration, dismissable = dismissable, queue = queue)
}
