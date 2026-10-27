package com.rjnsdev.linklift.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rjnsdev.linklift.app.AppScreen
import com.rjnsdev.linklift.app.LinkLiftViewModel
import com.rjnsdev.linklift.app.ui.YouTubeAuthActivity
import com.rjnsdev.linklift.app.MediaKind
import com.rjnsdev.linklift.app.formatBytes
import com.rjnsdev.linklift.app.formatDuration
import com.rjnsdev.linklift.app.selectedPreviewItem
import com.rjnsdev.linklift.app.ui.theme.LinkLiftAccent
import com.rjnsdev.linklift.app.ui.theme.LinkLiftCard
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextMuted
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextSecondary

@Composable
internal fun ShareOverlaySheet(
    viewModel: LinkLiftViewModel,
    onDismiss: () -> Unit,
    ensureNotificationPermission: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val youTubeAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshCookieStatus()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.closeActivityEvents.collect {
            onDismiss()
        }
    }

    BackHandler(enabled = true) {
        if (uiState.currentScreen == AppScreen.Processing) {
            viewModel.cancelAnalysis()
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (uiState.currentScreen == AppScreen.Processing) {
                    viewModel.cancelAnalysis()
                }
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* Intercept clicks so tapping inside does not dismiss */ },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = LinkLiftCard.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                    ) {}
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlatformBadge(url = uiState.preview?.sourceUrl ?: uiState.inputUrl)
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    IconButton(
                        onClick = {
                            if (uiState.currentScreen == AppScreen.Processing) {
                                viewModel.cancelAnalysis()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content based on screen state
                when (uiState.currentScreen) {
                    AppScreen.Processing -> {
                        ShareProcessingContent(
                            onCancel = {
                                viewModel.cancelAnalysis()
                                onDismiss()
                            }
                        )
                    }
                    AppScreen.Preview -> {
                        val preview = uiState.preview
                        if (preview != null) {
                            SharePreviewContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                ensureNotificationPermission = ensureNotificationPermission,
                            )
                        } else {
                            ShareProcessingContent(onCancel = onDismiss)
                        }
                    }
                    AppScreen.Batch -> {
                        ShareBatchContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            ensureNotificationPermission = ensureNotificationPermission,
                            onCancel = onDismiss,
                        )
                    }
                    else -> {
                        // If an error or returned to home screen without preview
                        ShareErrorContent(
                            message = uiState.message ?: "Unable to parse this link.",
                            onClose = onDismiss,
                        )
                    }
                }
            }
        }
    }

    if (uiState.showYouTubeAuthPrompt) {
        YouTubeAuthPromptDialog(
            reason = uiState.youTubeAuthPromptReason,
            onSignIn = {
                viewModel.dismissYouTubeAuthPrompt()
                youTubeAuthLauncher.launch(YouTubeAuthActivity.createIntent(context))
            },
            onOpenSettings = {
                viewModel.dismissYouTubeAuthPrompt()
                onDismiss()
            },
            onDismiss = {
                viewModel.dismissYouTubeAuthPrompt()
                onDismiss()
            },
        )
    }
}

@Composable
private fun ShareProcessingContent(
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = Color.White,
            strokeWidth = 3.dp,
        )
        Text(
            text = "Analyzing media link...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text = "Fetching available resolutions and formats",
            style = MaterialTheme.typography.bodySmall,
            color = LinkLiftTextSecondary,
        )
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        ) {
            Text("Cancel", color = Color.White)
        }
    }
}

@Composable
private fun SharePreviewContent(
    uiState: com.rjnsdev.linklift.app.LinkLiftUiState,
    viewModel: LinkLiftViewModel,
    ensureNotificationPermission: () -> Unit,
) {
    val preview = uiState.preview ?: return
    val currentIndex = uiState.selectedPreviewIndex
    val currentItem = selectedPreviewItem(preview, currentIndex)
    val hasMultipleItems = preview.items.size > 1
    val allFormats = currentItem.formats
    val displayFormats = if (uiState.showAudioOnly) {
        allFormats.filter { it.isAudioOnly }
    } else {
        allFormats
    }
    val hasFormats = allFormats.isNotEmpty()
    val hasAudioOnly = allFormats.any { it.isAudioOnly }
    val selectedFormatId = uiState.selectedFormatIdsByItem[currentIndex]
    val selectedFormat = allFormats.firstOrNull { it.formatId == selectedFormatId }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Media Hero Preview with data-saver toggle
        item {
            MediaHero(
                item = currentItem,
                selectedFormat = selectedFormat,
                enableOnlineVideoPreview = uiState.settings.enableOnlineVideoPreview,
                onFormatSelected = { fmt: MediaFormat -> viewModel.selectFormat(currentIndex, fmt.formatId) },
            )
        }

        // Title and Chips
        item {
            NeonCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = currentItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val platform = getPlatformBrand(preview.sourceUrl, preview.host)
                        if (platform != null) {
                            InfoChip(title = platform.first, value = "Platform", accentColor = platform.second)
                        } else {
                            InfoChip(title = preview.host, value = "Source")
                        }
                        InfoChip(title = formatBytes(currentItem.fileSizeBytes), value = "Size")
                        currentItem.durationMs?.let { InfoChip(title = formatDuration(it), value = "Duration") }
                        currentItem.resolution?.let { InfoChip(title = it, value = "Resolution") }
                    }
                }
            }
        }

        // Carousel selector if multiple items
        if (hasMultipleItems) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    preview.items.forEachIndexed { index, item ->
                        FilterChip(
                            selected = index == uiState.selectedPreviewIndex,
                            onClick = { viewModel.selectPreviewItem(index) },
                            label = {
                                Text(
                                    text = when (item.kind) {
                                        MediaKind.Video -> "Video ${index + 1}"
                                        MediaKind.Image -> "Photo ${index + 1}"
                                        MediaKind.Audio -> "Audio ${index + 1}"
                                        MediaKind.Unknown -> "Item ${index + 1}"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (item.kind) {
                                        MediaKind.Video -> Icons.Rounded.PlayArrow
                                        MediaKind.Image -> Icons.Rounded.Image
                                        MediaKind.Audio -> Icons.Rounded.GraphicEq
                                        MediaKind.Unknown -> Icons.Rounded.Collections
                                    },
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
            }
        }

        // Format Picker showing all video qualities
        item {
            FormatPickerCard(
                preview = preview,
                formats = displayFormats,
                hasAudioOnly = hasAudioOnly,
                hasAnyFormats = hasFormats,
                audioOnlyEnabled = uiState.showAudioOnly,
                selectedFormatId = selectedFormatId,
                preferredPreset = uiState.settings.preferredQuality,
                onAudioOnlyToggled = viewModel::setAudioOnlyFilter,
                onFormatSelected = { fmt: MediaFormat -> viewModel.selectFormat(currentIndex, fmt.formatId) },
            )
        }

        // Download Action Button
        item {
            Button(
                onClick = {
                    ensureNotificationPermission()
                    viewModel.queueDownload()
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F0F0F),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = if (selectedFormat?.isAudioOnly == true) Icons.Rounded.Audiotrack else Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        selectedFormat?.isAudioOnly == true -> "Download Audio"
                        selectedFormat != null -> "Download ${selectedFormat.label}"
                        else -> "Download Video"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (hasMultipleItems) {
            item {
                OutlinedButton(
                    onClick = {
                        ensureNotificationPermission()
                        viewModel.queueAllDownloads()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                ) {
                    Text("Download All Items (${preview.items.size})", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ShareBatchContent(
    uiState: com.rjnsdev.linklift.app.LinkLiftUiState,
    viewModel: LinkLiftViewModel,
    ensureNotificationPermission: () -> Unit,
    onCancel: () -> Unit,
) {
    val batch = uiState.batch ?: return
    val selectedCount = uiState.selectedBatchIndices.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = batch.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "$selectedCount of ${batch.entries.size} items selected",
            style = MaterialTheme.typography.bodyMedium,
            color = LinkLiftTextSecondary,
        )

        Button(
            onClick = {
                ensureNotificationPermission()
                viewModel.downloadSelectedBatchEntries()
            },
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF0F0F0F),
            ),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download $selectedCount Items", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        ) {
            Text("Cancel", color = Color.White)
        }
    }
}

@Composable
private fun ShareErrorContent(
    message: String,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White,
            ),
        ) {
            Text("Close")
        }
    }
}
