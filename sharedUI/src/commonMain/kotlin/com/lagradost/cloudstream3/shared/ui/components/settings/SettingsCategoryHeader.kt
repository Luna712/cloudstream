package com.lagradost.cloudstream3.shared.ui.components.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        color = CloudStreamTheme.colors.primary,
        style = MaterialTheme.typography.labelMedium,
    )
}
