package com.rjnsdev.linklift.app

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

internal fun shareDownloadedFile(
    context: Context,
    localUri: String?,
    mimeType: String?,
    title: String? = null,
) {
    if (localUri.isNullOrBlank()) {
        Toast.makeText(context, "File path is unavailable to share.", Toast.LENGTH_SHORT).show()
        return
    }

    runCatching {
        val resolvedUri = resolvableDownloadedUri(context, localUri)
        val resolvedMime = resolveDownloadedMimeType(context, resolvedUri, mimeType) ?: "*/*"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = resolvedMime
            putExtra(Intent.EXTRA_STREAM, resolvedUri)
            clipData = ClipData.newRawUri("media", resolvedUri)
            if (!title.isNullOrBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share ${title ?: "media"}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }.onFailure { e ->
        Toast.makeText(
            context,
            "Unable to share file: ${e.localizedMessage ?: "Unknown error"}",
            Toast.LENGTH_SHORT,
        ).show()
    }
}

internal fun copyTextToClipboard(
    context: Context,
    text: String,
    label: String = "Link",
) {
    if (text.isBlank()) return
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Failed to copy $label", Toast.LENGTH_SHORT).show()
    }
}

internal fun openDownloadedFile(context: Context, localUri: String, mimeType: String?) {
    val resolvedUri = resolvableDownloadedUri(context, localUri)
    val resolvedMimeType = resolveDownloadedMimeType(context, resolvedUri, mimeType)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        if (resolvedMimeType != null) {
            setDataAndType(resolvedUri, resolvedMimeType)
        } else {
            data = resolvedUri
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open file").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.onFailure {
        Toast.makeText(context, "Couldn't open this file outside LinkLift.", Toast.LENGTH_SHORT).show()
    }
}

internal fun isDownloadedMediaAvailable(context: Context, localUri: String?): Boolean {
    if (localUri.isNullOrBlank()) return false

    val trimmed = localUri.trim()
    val uri = when {
        trimmed.startsWith("/") -> Uri.fromFile(File(trimmed))
        else -> Uri.parse(trimmed)
    }

    return when (uri.scheme?.lowercase(Locale.ROOT)) {
        "file" -> {
            val path = uri.path
            if (path != null && runCatching {
                val f = File(path)
                f.exists() && f.length() > 0L
            }.getOrDefault(false)) {
                true
            } else {
                runCatching {
                    val resolved = resolvableDownloadedUri(context, trimmed)
                    context.contentResolver.openFileDescriptor(resolved, "r")?.use { pfd ->
                        pfd.statSize != 0L
                    } ?: context.contentResolver.openInputStream(resolved)?.use { true }
                    ?: false
                }.getOrDefault(false)
            }
        }
        "content" -> runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize != 0L
            } ?: context.contentResolver.openInputStream(uri)?.use { true }
            ?: false
        }.getOrDefault(false)
        else -> false
    }
}

internal fun resolvableDownloadedUri(context: Context, localUri: String): Uri {
    val trimmed = localUri.trim()
    val uri = when {
        trimmed.startsWith("/") -> Uri.fromFile(File(trimmed))
        else -> Uri.parse(trimmed)
    }
    if (!uri.scheme.equals("file", ignoreCase = true)) return uri

    val filePath = uri.path ?: return uri
    return runCatching {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(filePath),
        )
    }.getOrDefault(uri)
}

internal fun resolveDownloadedMimeType(
    context: Context,
    uri: Uri,
    mimeType: String?,
): String? {
    val normalizedMimeType = mimeType?.takeIf { it.isNotBlank() && it != "*/*" }
    if (normalizedMimeType != null) return normalizedMimeType

    context.contentResolver.getType(uri)?.let { return it }

    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        .orEmpty()
        .lowercase(Locale.ROOT)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

internal fun extractSubfolderFromUriString(uriString: String?): String? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val trimmed = uriString.trim()
        val rawPath = runCatching { java.net.URI(trimmed).path }.getOrNull()
            ?: runCatching { Uri.parse(trimmed).path }.getOrNull()
            ?: trimmed
        val decoded = runCatching { java.net.URLDecoder.decode(rawPath, "UTF-8") }.getOrDefault(rawPath)
        val marker = "LinkLift/"
        val index = decoded.indexOf(marker)
        if (index != -1) {
            val afterMarker = decoded.substring(index + marker.length).trimStart('/')
            if (afterMarker.contains('/')) {
                val subfolder = afterMarker.substringBeforeLast('/')
                MergeDownloadService.sanitizeSubfolder(subfolder).takeIf { it.isNotBlank() }
            } else {
                null
            }
        } else {
            null
        }
    }.getOrNull()
}

private fun deleteFileByPath(context: Context?, path: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false

    var deleted = runCatching { file.delete() }.getOrDefault(false)

    if (!deleted && file.exists() && context != null) {
        // Attempt deletion via MediaStore
        deleted = runCatching {
            val resolver = context.contentResolver
            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            }
            val count = resolver.delete(
                contentUri,
                "${MediaStore.MediaColumns.DATA} = ?",
                arrayOf(file.absolutePath),
            )
            count > 0 || !file.exists()
        }.getOrDefault(false)

        if (!deleted && file.exists()) {
            deleted = runCatching {
                val resolver = context.contentResolver
                val filesUri = MediaStore.Files.getContentUri("external")
                val count = resolver.delete(
                    filesUri,
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(file.absolutePath),
                )
                count > 0 || !file.exists()
            }.getOrDefault(false)
        }
    }

    if (deleted && context != null) {
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }
    }

    return deleted
}

private fun resolveAndAttemptDeleteFile(
    context: Context?,
    rawPath: String,
    fullUriString: String? = null,
): Boolean {
    // 1. Direct raw path
    if (File(rawPath).exists()) {
        return deleteFileByPath(context, rawPath)
    }

    // 2. java.net.URI path if URI provided or parseable (handles %20 while preserving +)
    if (!fullUriString.isNullOrBlank()) {
        val uriPath = runCatching { java.net.URI(fullUriString).path }.getOrNull()
        if (!uriPath.isNullOrBlank() && File(uriPath).exists()) {
            return deleteFileByPath(context, uriPath)
        }
    }

    // 3. Simple space decode (%20 -> space), preserving '+'
    val spaceDecoded = rawPath.replace("%20", " ")
    if (File(spaceDecoded).exists()) {
        return deleteFileByPath(context, spaceDecoded)
    }

    // 4. URLDecoder fallback
    val urlDecoded = runCatching { java.net.URLDecoder.decode(rawPath, "UTF-8") }.getOrNull()
    if (!urlDecoded.isNullOrBlank() && File(urlDecoded).exists()) {
        return deleteFileByPath(context, urlDecoded)
    }

    return false
}

internal fun deleteDownloadedPhysicalFile(context: Context?, localUri: String?): Boolean {
    if (localUri.isNullOrBlank()) return false
    var deleted = false
    runCatching {
        val trimmed = localUri.trim()
        when {
            trimmed.startsWith("/") -> {
                deleted = resolveAndAttemptDeleteFile(context, trimmed)
            }
            trimmed.startsWith("file://", ignoreCase = true) -> {
                val rawPath = trimmed.substring(7)
                deleted = resolveAndAttemptDeleteFile(context, rawPath, trimmed)
            }
            trimmed.startsWith("content://", ignoreCase = true) -> {
                if (context != null) {
                    val uri = Uri.parse(trimmed)

                    // If it is a DownloadProvider URI (e.g. content://downloads/all_downloads/<id> or my_downloads/<id>),
                    // resolve via DownloadManager to get the actual file path and MediaProvider URI.
                    if (trimmed.startsWith("content://downloads/", ignoreCase = true)) {
                        val downloadId = uri.lastPathSegment?.toLongOrNull()
                        if (downloadId != null) {
                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                            if (dm != null) {
                                val (localPath, mediaUri) = runCatching {
                                    dm.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                                        if (cursor.moveToFirst()) {
                                            val localCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                            val loc = if (localCol >= 0) cursor.getString(localCol) else null
                                            val mediaCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)
                                            } else -1
                                            val med = if (mediaCol >= 0) cursor.getString(mediaCol) else null
                                            Pair(loc, med)
                                        } else null
                                    }
                                }.getOrNull() ?: Pair(null, null)

                                if (!localPath.isNullOrBlank()) {
                                    val pathDeleted = deleteDownloadedPhysicalFile(context, localPath)
                                    if (pathDeleted) deleted = true
                                }
                                if (!mediaUri.isNullOrBlank()) {
                                    val mediaDeleted = deleteDownloadedPhysicalFile(context, mediaUri)
                                    if (mediaDeleted) deleted = true
                                }
                                runCatching { dm.remove(downloadId) }
                            }
                        }
                    }

                    val dataPath = runCatching {
                        context.contentResolver.query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DATA),
                            null,
                            null,
                            null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val col = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                                if (col >= 0) cursor.getString(col) else null
                            } else null
                        }
                    }.getOrNull()

                    val rowsDeleted = runCatching {
                        context.contentResolver.delete(uri, null, null)
                    }.getOrDefault(0)

                    if (rowsDeleted > 0) {
                        deleted = true
                    }

                    if (!dataPath.isNullOrBlank()) {
                        val file = File(dataPath)
                        if (file.exists()) {
                            val fileDeleted = runCatching { file.delete() }.getOrDefault(false)
                            if (fileDeleted) {
                                deleted = true
                            }
                        }
                        if (deleted) {
                            runCatching {
                                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                            }
                        }
                    }
                }
            }
            else -> {
                deleted = resolveAndAttemptDeleteFile(context, trimmed)
            }
        }
    }
    return deleted
}

