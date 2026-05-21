package com.lagradost.cloudstream3.shared.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.shared.generated.resources.Res
import com.lagradost.cloudstream3.shared.generated.resources.apply
import com.lagradost.cloudstream3.shared.generated.resources.cancel
import com.lagradost.cloudstream3.shared.ui.icons.check
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
                    val isSelected = selected.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                            ) {
                                if (isSelected) selected.remove(index)
                                else selected.add(index)
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = check,
                            contentDescription = null,
                            tint = if (isSelected) colors.primary
                                   else Color.Transparent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item,
                            color = colors.onBackground,
                            style = if (isSelected)
                                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            else
                                MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.toList()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(Res.string.apply))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, colors.onBackground.copy(alpha = 0.3f)),
            ) {
                Text(
                    text = stringResource(Res.string.cancel),
                    color = colors.onBackground,
                )
            }
        },
    )
}
