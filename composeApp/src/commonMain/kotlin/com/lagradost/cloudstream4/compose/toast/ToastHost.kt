package com.lagradost.cloudstream4.compose.toast

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.compose.theme.CloudStreamTheme

internal class ToastVisuals(val event: ToastEvent) : SnackbarVisuals {
    override val message: String = event.message
    override val actionLabel: String? = event.actionLabel
    override val duration: SnackbarDuration = event.duration
    override val withDismissAction: Boolean = event.dismissable
}

@Composable
internal fun ToastEffectHost(hostState: SnackbarHostState) {
    LaunchedEffect(hostState) {
        var showingMessage: String? = null
        ToastController.events.collect { event ->
            if (!event.queue) {
                hostState.currentSnackbarData?.dismiss()
                var latest = event
                var next = ToastController.drain()
                while (next != null) {
                    latest = next
                    next = ToastController.drain()
                }
                if (latest.message == showingMessage) return@collect
                showingMessage = latest.message
                val result = hostState.showSnackbar(ToastVisuals(latest))
                if (result == SnackbarResult.ActionPerformed) latest.onAction?.invoke()
                showingMessage = null
            } else {
                val result = hostState.showSnackbar(ToastVisuals(event))
                if (result == SnackbarResult.ActionPerformed) event.onAction?.invoke()
            }
        }
    }
}

@Composable
private fun ToastType.containerColor(): Color {
    val c = CloudStreamTheme.colors
    return when (this) {
        ToastType.Info -> c.background
        ToastType.Success -> c.primary.copy(alpha = 0.90f)
        ToastType.Warning -> Color(0xFFB45309)
        ToastType.Error -> Color(0xFFB91C1C)
    }
}

@Composable
private fun ToastType.contentColor(): Color = when (this) {
    ToastType.Info -> CloudStreamTheme.colors.onBackground
    else -> Color.White
}

@Composable
fun CloudStreamSnackbar(data: SnackbarData) {
    val type = (data.visuals as? ToastVisuals)?.event?.type ?: ToastType.Info
    Snackbar(
        snackbarData = data,
        shape = RoundedCornerShape(12.dp),
        containerColor = type.containerColor(),
        contentColor = type.contentColor(),
        actionColor = type.contentColor(),
        dismissActionContentColor = type.contentColor(),
    )
}
