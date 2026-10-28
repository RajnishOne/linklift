package com.rjnsdev.linklift.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rjnsdev.linklift.app.MergeDownloadService
import com.rjnsdev.linklift.app.ui.theme.LinkLiftCard
import com.rjnsdev.linklift.app.ui.theme.LinkLiftOutline
import com.rjnsdev.linklift.app.ui.theme.LinkLiftSurface
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextMuted
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextSecondary

@Composable
internal fun FolderSelectionDialog(
    title: String = "Select Download Location",
    selectedSubfolder: String,
    availableSubfolders: Set<String>,
    onSelectSubfolder: (String) -> Unit,
    onCreateSubfolder: (String) -> Unit,
    onDeleteSubfolder: ((String) -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    var newFolderName by remember { mutableStateOf("") }
    var activeSelection by remember(selectedSubfolder) {
        mutableStateOf(MergeDownloadService.sanitizeSubfolder(selectedSubfolder))
    }

    val sanitizedAvailable = remember(availableSubfolders, activeSelection) {
        buildSet {
            addAll(availableSubfolders.map { MergeDownloadService.sanitizeSubfolder(it) }.filter { it.isNotBlank() })
            if (activeSelection.isNotBlank()) add(activeSelection)
        }.sorted()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = LinkLiftSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                val currentPath = if (activeSelection.isBlank()) {
                    "Downloads/LinkLift"
                } else {
                    "Downloads/LinkLift/$activeSelection"
                }
                Text(
                    text = "Saving to: $currentPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = LinkLiftTextSecondary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FolderOptionRow(
                            name = "Main folder (LinkLift)",
                            fullPath = "Downloads/LinkLift",
                            isSelected = activeSelection.isBlank(),
                            isRoot = true,
                            onSelect = {
                                activeSelection = ""
                                onSelectSubfolder("")
                            },
                            onDelete = null,
                        )
                    }

                    items(sanitizedAvailable) { folderName ->
                        FolderOptionRow(
                            name = folderName,
                            fullPath = "Downloads/LinkLift/$folderName",
                            isSelected = activeSelection.equals(folderName, ignoreCase = true),
                            isRoot = false,
                            onSelect = {
                                activeSelection = folderName
                                onSelectSubfolder(folderName)
                            },
                            onDelete = if (onDeleteSubfolder != null) {
                                {
                                    if (activeSelection.equals(folderName, ignoreCase = true)) {
                                        activeSelection = ""
                                        onSelectSubfolder("")
                                    }
                                    onDeleteSubfolder(folderName)
                                }
                            } else null,
                        )
                    }
                }

                // Inline New Folder Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Create new subfolder",
                        style = MaterialTheme.typography.labelMedium,
                        color = LinkLiftTextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g. Music, Math", color = LinkLiftTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = LinkLiftOutline,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                            ),
                        )
                        IconButton(
                            onClick = {
                                val clean = MergeDownloadService.sanitizeSubfolder(newFolderName)
                                if (clean.isNotBlank()) {
                                    onCreateSubfolder(clean)
                                    activeSelection = clean
                                    onSelectSubfolder(clean)
                                    newFolderName = ""
                                }
                            },
                            enabled = newFolderName.isNotBlank(),
                            modifier = Modifier
                                .background(
                                    if (newFolderName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.1f),
                                    CircleShape,
                                )
                                .size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add folder",
                                tint = if (newFolderName.isNotBlank()) Color(0xFF0F0F0F) else LinkLiftTextMuted,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F0F0F),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun FolderOptionRow(
    name: String,
    fullPath: String,
    isSelected: Boolean,
    isRoot: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.14f) else LinkLiftCard,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color.White.copy(alpha = 0.6f) else LinkLiftOutline.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.08f),
            ) {
                Icon(
                    imageVector = if (isRoot) Icons.Rounded.FolderSpecial else Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF0F0F0F) else Color.White,
                    modifier = Modifier.padding(6.dp).size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = fullPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = LinkLiftTextMuted,
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (onDelete != null && !isRoot) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete folder",
                        tint = LinkLiftTextMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
