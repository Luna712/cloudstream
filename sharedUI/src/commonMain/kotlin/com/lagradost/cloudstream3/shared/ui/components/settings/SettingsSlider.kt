package com.lagradost.cloudstream3.shared.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showValue: Boolean = false,
) {
    val colors = CloudStreamTheme.colors
    val contentAlpha = if (enabled) 1f else 0.38f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.icon.copy(alpha = contentAlpha),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.onBackground.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = colors.onBackground.copy(alpha = 0.6f * contentAlpha),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (showValue) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = value.toInt().toString(),
                    color = colors.onBackground.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.primary.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (icon != null) 40.dp else 0.dp),
        )
    }
}
