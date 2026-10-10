package com.rjnsdev.linklift.app

import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun selectedPreviewItem(
    preview: MediaPreview,
    index: Int,
): PreviewMediaItem {
    return preview.items.getOrElse(index.coerceAtLeast(0)) {
        PreviewMediaItem(
            resolvedUrl = preview.resolvedUrl,
            title = preview.title,
            description = preview.description,
            mimeType = preview.mimeType,
            kind = preview.kind,
            fileSizeBytes = preview.fileSizeBytes,
            durationMs = preview.durationMs,
            resolution = preview.resolution,
            fileName = preview.fileName,
        )
    }
}

internal fun multiItemFileName(
    originalFileName: String,
    index: Int,
): String {
    val dotIndex = originalFileName.lastIndexOf('.')
    return if (dotIndex > 0) {
        "${originalFileName.substring(0, dotIndex)}_${index + 1}${originalFileName.substring(dotIndex)}"
    } else {
        "${originalFileName}_${index + 1}"
    }
}

internal fun buildSafeFileName(
    title: String?,
    ext: String?,
    fallbackUrl: String? = null,
    mimeType: String? = null,
): String {
    val cleanExt = ext?.trim()?.removePrefix(".")?.takeIf { it.isNotBlank() }
        ?: mimeType?.let { runCatching { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }.getOrNull() }
        ?: "mp4"

    val baseTitle = title?.trim()?.takeIf { it.isNotBlank() }
    val sanitizedTitle = if (baseTitle != null) {
        baseTitle.replace(Regex("""[\\/:*?"<>|%#\r\n\t]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('.', ' ', '_')
            .take(100)
    } else null

    if (!sanitizedTitle.isNullOrBlank() &&
        !sanitizedTitle.equals("videoplayback", ignoreCase = true) &&
        !sanitizedTitle.equals("watch", ignoreCase = true) &&
        !sanitizedTitle.equals("index", ignoreCase = true) &&
        !sanitizedTitle.equals("media", ignoreCase = true)
    ) {
        return "$sanitizedTitle.$cleanExt"
    }

    if (!fallbackUrl.isNullOrBlank()) {
        val pathSegment = fallbackUrl.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val guessed = if (pathSegment.isNotBlank() && pathSegment.contains('.')) {
            pathSegment
        } else {
            runCatching { URLUtil.guessFileName(fallbackUrl, null, mimeType) }.getOrNull()
        }
        if (!guessed.isNullOrBlank() &&
            !guessed.startsWith("videoplayback", ignoreCase = true) &&
            !guessed.startsWith("watch", ignoreCase = true) &&
            !guessed.startsWith("downloadfile", ignoreCase = true)
        ) {
            val guessedBase = guessed.substringBeforeLast('.')
            if (guessedBase.isNotBlank() && !guessedBase.startsWith("AQP", ignoreCase = true)) {
                return "$guessedBase.$cleanExt"
            }
        }
    }

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return "linklift_${timestamp}.$cleanExt"
}
