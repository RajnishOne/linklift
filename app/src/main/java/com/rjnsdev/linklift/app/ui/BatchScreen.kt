package com.rjnsdev.linklift.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rjnsdev.linklift.app.QualityPreset
import com.rjnsdev.linklift.app.ui.BatchQualityDialog
import com.rjnsdev.linklift.app.ui.FolderSelectionDialog
import com.rjnsdev.linklift.app.ui.theme.LinkLiftAccent
import com.rjnsdev.linklift.app.ui.theme.LinkLiftAccentBright
import com.rjnsdev.linklift.app.ui.theme.LinkLiftCard
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextMuted
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextSecondary

@Composable
internal fun BatchScreen(
    uiState: LinkLiftUiState,
    onToggleEntry: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onDownload: () -> Unit,
    onSelectTargetSubfolder: (String) -> Unit,
    onCreateSubfolder: (String) -> Unit,
    onAudioOnlyToggled: (Boolean) -> Unit = {},
    onOpenQualityDialog: () -> Unit = {},
    onSelectQualityPreset: (QualityPreset) -> Unit = {},
    onDismissQualityDialog: () -> Unit = {},
) {
    val batch = uiState.batch ?: return
    val selected = uiState.selectedBatchIndices
    val selectedCount = selected.size
    val progress = uiState.batchProgress
    val isProcessing = progress?.isActive == true
    var showFolderDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    if (showFolderDialog) {
        FolderSelectionDialog(
            title = "Choose Batch Folder",
            selectedSubfolder = uiState.targetSubfolder,
            availableSubfolders = uiState.settings.customSubfolders,
            onSelectSubfolder = onSelectTargetSubfolder,
            onCreateSubfolder = onCreateSubfolder,
            onDismissRequest = { showFolderDialog = false },
        )
    }

    if (uiState.showBatchQualityDialog) {
        val currentPreset = uiState.batchQualityPreset
            ?: uiState.settings.preferredQuality.takeIf { it != QualityPreset.AskEveryTime }
            ?: QualityPreset.BestQuality

        BatchQualityDialog(
            selectedPreset = currentPreset,
            onSelectPreset = onSelectQualityPreset,
            onDismissRequest = onDismissQualityDialog,
        )
    }

    val destinationPath = if (uiState.targetSubfolder.isBlank()) {
        "Download/LinkLift"
    } else {
        "Download/LinkLift/${uiState.targetSubfolder}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = if (selectedCount > 0) 84.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                NeonCard(contentPadding = PaddingValues(14.dp), shape = RoundedCornerShape(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = batch.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = batch.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = when (batch.source) {
                                    BatchSource.Playlist -> Icons.Rounded.PlayCircleOutline
                                    BatchSource.BulkPaste -> Icons.Rounded.Collections
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        if (batch.uploader.isNotBlank()) {
                            Text(
                                text = batch.uploader,
                                style = MaterialTheme.typography.bodySmall,
                                color = LinkLiftTextSecondary,
                            )
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InfoChip(title = "${batch.returnedCount}", value = "Items shown", compact = true)
                            if (batch.totalCount > batch.returnedCount) {
                                InfoChip(title = "${batch.totalCount}", value = "Total available", compact = true)
                            }
                            InfoChip(title = "$selectedCount", value = "Selected", compact = true)
                        }
                        if (batch.isTruncated) {
                            Text(
                                text = "Showing first ${batch.returnedCount} of ${batch.totalCount} items. Open source link to download the rest.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LinkLiftTextSecondary,
                            )
                        }
                        if (batch.entries.size > BATCH_AUTO_SELECT_LIMIT) {
                            Text(
                                text = "Large batch. Nothing selected by default. Check the items you want to download.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LinkLiftTextSecondary,
                            )
                        }
                    }
                }
            }

            if (progress != null) {
                item {
                    NeonCard(contentPadding = PaddingValues(14.dp), shape = RoundedCornerShape(18.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isProcessing) "Resolving links..." else "Batch complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            LinearProgressIndicator(
                                progress = {
                                    if (progress.total <= 0) 0f
                                    else (progress.processed.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.08f),
                            )
                            Text(
                                text = "${progress.processed} of ${progress.total} • ${progress.succeeded} queued • ${progress.failed} failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = LinkLiftTextSecondary,
                            )
                            progress.currentTitle?.takeIf { isProcessing && it.isNotBlank() }?.let { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LinkLiftTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            item {
                NeonCard(contentPadding = PaddingValues(12.dp), shape = RoundedCornerShape(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Format",
                            style = MaterialTheme.typography.labelMedium,
                            color = LinkLiftTextSecondary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val isAudio = uiState.batchAudioOnly
                            val videoSubtitle = when {
                                uiState.batchQualityPreset != null -> uiState.batchQualityPreset.label
                                uiState.settings.preferredQuality == QualityPreset.AskEveryTime -> "Ask every time (tap to set)"
                                else -> uiState.settings.preferredQuality.label
                            }

                            // Video option
                            Surface(
                                onClick = {
                                    if (!isProcessing) {
                                        if (isAudio) {
                                            onAudioOnlyToggled(false)
                                        } else {
                                            onOpenQualityDialog()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isAudio) Color.White.copy(alpha = 0.15f) else LinkLiftCard.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (!isAudio) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Videocam,
                                        contentDescription = "Video",
                                        tint = if (!isAudio) Color.White else LinkLiftTextSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Video",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (!isAudio) FontWeight.Bold else FontWeight.Medium,
                                            color = if (!isAudio) Color.White else LinkLiftTextSecondary,
                                        )
                                        Text(
                                            text = videoSubtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LinkLiftTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (!isAudio) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = "Change quality",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            // Audio option
                            Surface(
                                onClick = { if (!isProcessing) onAudioOnlyToggled(true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAudio) Color.White.copy(alpha = 0.15f) else LinkLiftCard.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAudio) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Audiotrack,
                                        contentDescription = "Audio only",
                                        tint = if (isAudio) Color.White else LinkLiftTextSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Audio only",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isAudio) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAudio) Color.White else LinkLiftTextSecondary,
                                        )
                                        Text(
                                            text = uiState.settings.preferredAudioFormat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LinkLiftTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY ACTIONS MOVED TO TOP!
            item {
                NeonCard(contentPadding = PaddingValues(12.dp), shape = RoundedCornerShape(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            enabled = !isProcessing && selectedCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F0F0F),
                                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                disabledContentColor = LinkLiftTextMuted,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = if (uiState.batchAudioOnly) Icons.Rounded.Audiotrack else Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = if (!isProcessing && selectedCount > 0) Color(0xFF0F0F0F) else LinkLiftTextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isProcessing) {
                                    "Queuing downloads..."
                                } else if (selectedCount > 0) {
                                    val kindLabel = if (uiState.batchAudioOnly) "audio" else "item"
                                    "Download $selectedCount $kindLabel${if (selectedCount == 1) "" else "s"}"
                                } else {
                                    "Select items to download"
                                },
                                color = if (!isProcessing && selectedCount > 0) Color(0xFF0F0F0F) else LinkLiftTextMuted,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        DownloadDestinationChip(
                            destinationPath = destinationPath,
                            onClick = { if (!isProcessing) showFolderDialog = true },
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = onSelectAll,
                                    enabled = !isProcessing && selectedCount < batch.entries.size,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text("Select all (${batch.entries.size})", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(
                                    onClick = onClear,
                                    enabled = !isProcessing && selectedCount > 0,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            TextButton(
                                onClick = onCancel,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(if (isProcessing) "Stop batch" else "Cancel", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Entries (${batch.entries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = LinkLiftTextSecondary,
                    )
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            itemsIndexed(batch.entries) { index, entry ->
                BatchEntryRow(
                    entry = entry,
                    isChecked = index in selected,
                    isProcessing = isProcessing,
                    onCheckedChange = { onToggleEntry(index) },
                )
            }
        }

        // Sticky bottom download pill when scrolled past top controls
        val showStickyBar = remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 3 && selectedCount > 0 && !isProcessing
            }
        }
        AnimatedVisibility(
            visible = showStickyBar.value,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E1E1E),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 8.dp,
            ) {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F0F0F),
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = if (uiState.batchAudioOnly) Icons.Rounded.Audiotrack else Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val kindLabel = if (uiState.batchAudioOnly) "audio" else "item"
                    Text(
                        text = "Download $selectedCount $kindLabel${if (selectedCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchEntryRow(
    entry: LinkBatchEntry,
    isChecked: Boolean,
    isProcessing: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    NeonCard(
        modifier = if (isProcessing) Modifier else Modifier.clickable { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    ) {
        val platform = getPlatformBrand(entry.sourceUrl, entry.host)
        val brandColor = platform?.color ?: Color.White
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = brandColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, brandColor.copy(alpha = 0.45f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = platform?.icon ?: Icons.Rounded.PlayCircleOutline,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metaLine = buildList {
                    if (entry.uploader.isNotBlank()) add(entry.uploader)
                    entry.durationMs?.let { add(formatDuration(it)) }
                    if (entry.host.isNotBlank()) add(entry.host)
                }.joinToString(" • ")
                if (metaLine.isNotBlank()) {
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = LinkLiftTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = entry.sourceUrl,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = LinkLiftTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = !isProcessing,
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    checkmarkColor = Color(0xFF0F0F0F),
                    uncheckedColor = LinkLiftTextSecondary,
                ),
            )
        }
    }
}
