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

internal fun chooseAudioFormat(
    audioFormats: List<MediaFormat>,
    audioPref: AudioFormatPreference = AudioFormatPreference.Any,
): MediaFormat? {
    if (audioFormats.isEmpty()) return null
    fun matchesAudioPref(format: MediaFormat): Boolean {
        return when (audioPref) {
            AudioFormatPreference.Any -> true
            AudioFormatPreference.M4a -> format.ext.equals("m4a", ignoreCase = true)
            AudioFormatPreference.Opus -> format.ext.equals("webm", ignoreCase = true) || format.ext.equals("opus", ignoreCase = true)
            AudioFormatPreference.Mp3 -> format.ext.equals("mp3", ignoreCase = true)
        }
    }
    val matching = audioFormats.filter { matchesAudioPref(it) }
    val pool = if (matching.isNotEmpty()) matching else audioFormats
    return pool.maxByOrNull { (it.abr ?: 0) * 1000L + (it.tbr ?: 0) } ?: pool.first()
}

internal fun chooseFormatForPreset(
    formats: List<MediaFormat>,
    preset: QualityPreset,
    videoPref: VideoFormatPreference = VideoFormatPreference.Any,
    audioPref: AudioFormatPreference = AudioFormatPreference.Any,
    preferAudioOnly: Boolean = false,
): MediaFormat? {
    if (formats.isEmpty()) return null

    val audioOnlyFormats = formats.filter { it.isAudioOnly }
    val videoFormats = formats.filter { !it.isAudioOnly }

    // If audio-only is preferred or this collection only contains audio streams
    if (preferAudioOnly && audioOnlyFormats.isNotEmpty()) {
        return chooseAudioFormat(audioOnlyFormats, audioPref)
    }
    if (videoFormats.isEmpty()) {
        return chooseAudioFormat(audioOnlyFormats, audioPref)
    }

    fun pickBestCandidate(candidates: List<MediaFormat>): MediaFormat? {
        if (candidates.isEmpty()) return null
        return when (videoPref) {
            VideoFormatPreference.Mp4 -> {
                candidates.filter { it.ext.equals("mp4", ignoreCase = true) }.maxByOrNull { it.tbr ?: 0 }
                    ?: candidates.maxByOrNull { it.tbr ?: 0 }
            }
            VideoFormatPreference.Webm -> {
                candidates.filter { it.ext.equals("webm", ignoreCase = true) }.maxByOrNull { it.tbr ?: 0 }
                    ?: candidates.maxByOrNull { it.tbr ?: 0 }
            }
            VideoFormatPreference.Any -> {
                // When preference is Any, prefer MP4 for universal Android playback/sharing compatibility
                candidates.filter { it.ext.equals("mp4", ignoreCase = true) }.maxByOrNull { it.tbr ?: 0 }
                    ?: candidates.maxByOrNull { it.tbr ?: 0 }
            }
        }
    }

    fun videoScore(format: MediaFormat): Long {
        val prefBonus = when (videoPref) {
            VideoFormatPreference.Mp4 -> if (format.ext.equals("mp4", ignoreCase = true)) 10_000_000L else 0L
            VideoFormatPreference.Webm -> if (format.ext.equals("webm", ignoreCase = true)) 10_000_000L else 0L
            VideoFormatPreference.Any -> if (format.ext.equals("mp4", ignoreCase = true)) 5_000_000L else 0L
        }
        val resScore = (format.height ?: 0) * 100_000L
        val bitrateScore = (format.tbr ?: 0).toLong()
        return prefBonus + resScore + bitrateScore
    }

    val preferredPool = when (videoPref) {
        VideoFormatPreference.Mp4 -> videoFormats.filter { it.ext.equals("mp4", ignoreCase = true) }.ifEmpty { videoFormats }
        VideoFormatPreference.Webm -> videoFormats.filter { it.ext.equals("webm", ignoreCase = true) }.ifEmpty { videoFormats }
        VideoFormatPreference.Any -> videoFormats
    }

    return when (preset) {
        QualityPreset.SmallestFile -> {
            val withSize = preferredPool.filter { it.fileSizeBytes != null && it.fileSizeBytes > 0 }
            if (withSize.isNotEmpty()) {
                withSize.minByOrNull { it.fileSizeBytes ?: Long.MAX_VALUE }
            } else {
                preferredPool.minByOrNull { (it.height ?: Int.MAX_VALUE) * 100_000L + (it.tbr ?: Int.MAX_VALUE) }
            }
        }
        QualityPreset.BestQuality, QualityPreset.AskEveryTime -> {
            val maxHeight = preferredPool.mapNotNull { it.height }.maxOrNull() ?: 0
            val candidatesAtMax = preferredPool.filter { (it.height ?: 0) == maxHeight }
            pickBestCandidate(candidatesAtMax)
                ?: preferredPool.maxByOrNull { videoScore(it) }
                ?: preferredPool.first()
        }
        QualityPreset.Res1080p, QualityPreset.Res720p, QualityPreset.Res480p, QualityPreset.Res360p -> {
            val target = preset.targetHeight ?: 1080
            val exactMatches = videoFormats.filter { (it.height ?: 0) == target }
            if (exactMatches.isNotEmpty()) {
                pickBestCandidate(exactMatches)
            } else {
                val lowerMatches = videoFormats.filter { (it.height ?: 0) <= target }
                val candidatePool = lowerMatches.ifEmpty { videoFormats }
                val bestHeight = candidatePool.mapNotNull { it.height }.maxOrNull() ?: 0
                val atBestHeight = candidatePool.filter { (it.height ?: 0) == bestHeight }
                pickBestCandidate(atBestHeight) ?: candidatePool.firstOrNull()
            } ?: videoFormats.maxByOrNull { videoScore(it) } ?: videoFormats.first()
        }
    }
}
