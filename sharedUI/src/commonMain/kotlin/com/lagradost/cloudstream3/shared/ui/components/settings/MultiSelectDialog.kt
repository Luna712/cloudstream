package com.lagradost.cloudstream3.shared.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.generated.resources.Res
import com.lagradost.cloudstream3.shared.generated.resources.cancel
import com.lagradost.cloudstream3.shared.generated.resources.confirm
import com.lagradost.cloudstream3.shared.ui.theme.CloudStreamTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun MultiSelectDialog(
    title: String,
    items: List<String>,
    selectedIndices: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    val colors = CloudStreamTheme.colors
    val selected = remember { mutableStateListOf<Int>().also { it.addAll(selectedIndices) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceVariant,
        title = {
            Text(
                text = title,
                color = colors.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                            ) {
                                if (selected.contains(index)) selected.remove(index)
                                else selected.add(index)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selected.contains(index),
                            onCheckedChange = {
                                if (it) selected.add(index) else selected.remove(index)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primary,
                                uncheckedColor = colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            color = colors.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected.toList()) }) {
                Text(
                    text = stringResource(Res.string.confirm),
                    color = colors.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.cancel),
                    color = colors.onBackground.copy(alpha = 0.6f),
                )
            }
        },
    )
}
