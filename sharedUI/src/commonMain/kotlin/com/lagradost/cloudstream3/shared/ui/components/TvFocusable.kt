package com.lagradost.cloudstream3.shared.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme

/**
 * Reusable TV focusable modifier with built-in focus border.
 *
 * @param isTV Whether to use TV focus behavior
 * @param onClick Action to perform when item is clicked/selected
 * @param focusRequester Optional external FocusRequester (e.g. for default focus on first item)
 * @param onFocusChanged Optional callback when focus state changes
 * @param shape Shape of the focus border
 */
@Composable
fun Modifier.tvFocusable(
    isTV: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
): Modifier {
    val colors = CloudStreamTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    val focusRequesterLocal = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: focusRequesterLocal

    return this
        .focusRequester(effectiveFocusRequester)
        .onFocusChanged {
            isFocused = it.isFocused
            onFocusChanged?.invoke(it.isFocused)
        }
        .focusable()
        .border(
            width = if (isFocused && isTV) 2.dp else 0.dp,
            color = if (isFocused && isTV) colors.onBackground else Color.Transparent,
            shape = shape,
        )
        .pointerInput(isFocused, isTV) {
            detectTapGestures {
                if (isTV && !isFocused) {
                    effectiveFocusRequester.requestFocus()
                } else {
                    onClick()
                }
            }
        }
}
