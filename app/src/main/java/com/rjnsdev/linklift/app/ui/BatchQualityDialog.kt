package com.rjnsdev.linklift.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rjnsdev.linklift.app.QualityPreset
import com.rjnsdev.linklift.app.ui.theme.LinkLiftCard
import com.rjnsdev.linklift.app.ui.theme.LinkLiftSurface
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextSecondary

private val BATCH_SELECTABLE_PRESETS = listOf(
    QualityPreset.BestQuality,
    QualityPreset.Res1080p,
    QualityPreset.Res720p,
    QualityPreset.Res480p,
    QualityPreset.Res360p,
    QualityPreset.SmallestFile,
)

@Composable
internal fun BatchQualityDialog(
    selectedPreset: QualityPreset,
    onSelectPreset: (QualityPreset) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = LinkLiftSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Select Video Quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Choose quality for all items in this playlist/batch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LinkLiftTextSecondary,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(BATCH_SELECTABLE_PRESETS) { preset ->
                    val isSelected = preset == selectedPreset
                    val borderColor = if (isSelected) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    val backgroundColor = if (isSelected) Color.White.copy(alpha = 0.12f) else LinkLiftCard.copy(alpha = 0.7f)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectPreset(preset)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = backgroundColor,
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = when (preset) {
                                    QualityPreset.BestQuality -> Icons.Rounded.Bolt
                                    QualityPreset.Res1080p, QualityPreset.Res720p -> Icons.Rounded.Hd
                                    QualityPreset.Res480p, QualityPreset.Res360p -> Icons.Rounded.PlayCircleOutline
                                    QualityPreset.SmallestFile -> Icons.Rounded.Speed
                                    QualityPreset.AskEveryTime -> Icons.Rounded.Bolt
                                },
                                contentDescription = null,
                                tint = Color.White,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LinkLiftTextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        },
    )
}
