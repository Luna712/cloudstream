package com.lagradost.cloudstream3.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme

/**
 * Reusable TV focus border modifier.
 * Applies a white border and subtle background highlight when focused on TV.
 * Use on any composable that needs TV D-pad focus indication.
 */
@Composable
fun Modifier.tvFocusBorder(
    isFocused: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
): Modifier {
    val colors = CloudStreamTheme.colors
    return this
        .background(
            color = if (isFocused) colors.primary.copy(alpha = 0.15f) else Color.Transparent,
            shape = shape,
        )
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) colors.onBackground else Color.Transparent,
            shape = shape,
        )
}
