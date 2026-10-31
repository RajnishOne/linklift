package com.rjnsdev.linklift.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
