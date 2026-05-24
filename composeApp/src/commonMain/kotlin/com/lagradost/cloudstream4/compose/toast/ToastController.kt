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
)

object ToastController {
    private val _events = Channel<ToastEvent>(Channel.BUFFERED)
    internal val events = _events.receiveAsFlow()

    suspend fun show(
        message: String,
        type: ToastType = ToastType.Info,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) = _events.send(ToastEvent(message, type, duration, actionLabel, onAction))

    suspend fun showSuccess(message: String, duration: SnackbarDuration = SnackbarDuration.Short) =
        show(message, ToastType.Success, duration)

    suspend fun showWarning(message: String, duration: SnackbarDuration = SnackbarDuration.Short) =
        show(message, ToastType.Warning, duration)

    suspend fun showError(message: String, duration: SnackbarDuration = SnackbarDuration.Long) =
        show(message, ToastType.Error, duration)
}
