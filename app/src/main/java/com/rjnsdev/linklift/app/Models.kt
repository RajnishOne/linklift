package com.rjnsdev.linklift.app

import java.util.Locale

internal const val BATCH_MAX_ITEMS = 200
internal const val BATCH_AUTO_SELECT_LIMIT = 50

enum class AppScreen {
    Home,
    Processing,
    Preview,
    Batch,
    Downloads,
    Settings,
}

enum class BatchSource {
    Playlist,
    BulkPaste,
}

enum class MediaKind {
    Video,
    Audio,
    Image,
    Unknown,
    ;

    companion object {
        fun fromMimeType(mimeType: String?, url: String): MediaKind {
            val normalizedMime = mimeType.orEmpty().lowercase(Locale.ROOT)
            val normalizedUrl = url.lowercase(Locale.ROOT)
            return when {
                normalizedMime.startsWith("video/") -> Video
                normalizedMime.startsWith("audio/") -> Audio
                normalizedMime.startsWith("image/") -> Image
                normalizedUrl.endsWith(".mp4") || normalizedUrl.endsWith(".mkv") ||
                    normalizedUrl.endsWith(".webm") || normalizedUrl.endsWith(".m3u8") -> Video
                normalizedUrl.endsWith(".mp3") || normalizedUrl.endsWith(".wav") ||
                    normalizedUrl.endsWith(".aac") || normalizedUrl.endsWith(".m4a") -> Audio
                normalizedUrl.endsWith(".jpg") || normalizedUrl.endsWith(".jpeg") ||
                    normalizedUrl.endsWith(".png") || normalizedUrl.endsWith(".webp") ||
                    normalizedUrl.endsWith(".gif") -> Image
                else -> Unknown
            }
        }
    }
}

enum class DownloadState {
    Queued,
    Downloading,
    Completed,
    Failed,
    Paused,
}

enum class QualityPreset(
    val storageKey: String,
    val label: String,
    val description: String,
    val targetHeight: Int? = null,
) {
    BestQuality(
        storageKey = "best_quality",
        label = "Best quality",
        description = "Always pick the highest-resolution stream available.",
        targetHeight = null,
    ),
    Res1080p(
        storageKey = "1080p",
        label = "1080p (Full HD)",
        description = "Target 1080p resolution or closest available match.",
        targetHeight = 1080,
    ),
    Res720p(
        storageKey = "720p",
        label = "720p (HD)",
        description = "Target 720p resolution or closest available match.",
        targetHeight = 720,
    ),
    Res480p(
        storageKey = "480p",
        label = "480p (SD)",
        description = "Target 480p standard resolution to save data.",
        targetHeight = 480,
    ),
    Res360p(
        storageKey = "360p",
        label = "360p (Data saver)",
        description = "Target 360p resolution for minimal storage and bandwidth.",
        targetHeight = 360,
    ),
    SmallestFile(
        storageKey = "smallest_file",
        label = "Smallest file",
        description = "Optimize for the lowest data usage and smallest download size.",
        targetHeight = null,
    ),
    AskEveryTime(
        storageKey = "ask_every_time",
        label = "Ask every time",
        description = "Show the format picker on every analyze so you can choose per link.",
        targetHeight = null,
    );

    companion object {
        fun fromStorageKey(key: String?): QualityPreset =
            entries.firstOrNull { it.storageKey == key } ?: BestQuality
    }
}

enum class VideoFormatPreference(val storageKey: String, val label: String, val ext: String) {
    Any(storageKey = "any", label = "Auto", ext = ""),
    Mp4(storageKey = "mp4", label = "MP4", ext = "mp4"),
    Webm(storageKey = "webm", label = "WebM", ext = "webm");

    companion object {
        fun fromStorageKey(key: String?): VideoFormatPreference =
            entries.firstOrNull { it.storageKey == key } ?: Any
    }
}

enum class AudioFormatPreference(val storageKey: String, val label: String, val ext: String) {
    Any(storageKey = "any", label = "Auto", ext = ""),
    M4a(storageKey = "m4a", label = "M4A", ext = "m4a"),
    Opus(storageKey = "opus", label = "WebM / Opus", ext = "webm"),
    Mp3(storageKey = "mp3", label = "MP3", ext = "mp3");

    companion object {
        fun fromStorageKey(key: String?): AudioFormatPreference =
            entries.firstOrNull { it.storageKey == key } ?: Any
    }
}

data class UserPreferences(
    val wifiOnly: Boolean = false,
    val completionNotifications: Boolean = true,
    val preferredQuality: QualityPreset = QualityPreset.BestQuality,
    val preferredVideoFormat: VideoFormatPreference = VideoFormatPreference.Any,
    val preferredAudioFormat: AudioFormatPreference = AudioFormatPreference.Any,
    val enableOnlineVideoPreview: Boolean = true,
    val downloadLocation: String = "Downloads/LinkLift",
    val downloadSubfolder: String = "",
    val customSubfolders: Set<String> = emptySet(),
    val hasYouTubeCookies: Boolean = false,
    val youtubeCookiesLastModified: Long = 0L,
) {
    val effectiveDownloadPath: String
        get() = if (downloadSubfolder.isBlank()) "Downloads/LinkLift" else "Downloads/LinkLift/${downloadSubfolder.trim().trim('/')}"
}

data class MediaFormat(
    val formatId: String,
    val label: String,
    val mediaUrl: String,
    val mimeType: String,
    val kind: MediaKind,
    val ext: String,
    val height: Int?,
    val width: Int?,
    val fps: Int?,
    val abr: Int?,
    val tbr: Int?,
    val fileSizeBytes: Long?,
    val fileSizeApprox: Boolean,
    val isAudioOnly: Boolean,
    val isProgressive: Boolean,
    val isVideoOnly: Boolean = false,
    val vcodec: String? = null,
    val acodec: String? = null,
    val httpHeaders: Map<String, String> = emptyMap(),
    val mergeAudio: MediaFormat? = null,
) {
    val requiresMerge: Boolean get() = isVideoOnly && mergeAudio != null
}

data class PreviewMediaItem(
    val resolvedUrl: String,
    val title: String,
    val description: String,
    val mimeType: String,
    val kind: MediaKind,
    val fileSizeBytes: Long?,
    val durationMs: Long?,
    val resolution: String?,
    val fileName: String,
    val formats: List<MediaFormat> = emptyList(),
    val thumbnailUrl: String? = null,
)

data class MediaPreview(
    val sourceUrl: String,
    val resolvedUrl: String,
    val title: String,
    val description: String,
    val mimeType: String,
    val kind: MediaKind,
    val host: String,
    val fileSizeBytes: Long?,
    val durationMs: Long?,
    val resolution: String?,
    val fileName: String,
    val isDirectLink: Boolean = true,
    val items: List<PreviewMediaItem> = emptyList(),
    val thumbnailUrl: String? = null,
)

data class DownloadEntry(
    val id: Long,
    val title: String,
    val description: String,
    val mimeType: String?,
    val kind: MediaKind,
    val state: DownloadState,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val localUri: String?,
    val sourceUrl: String?,
    val updatedAt: Long,
    /** [DownloadManager.COLUMN_REASON] when state is [DownloadState.Failed]; otherwise null. */
    val dmFailureCode: Int? = null,
)

data class LinkBatchEntry(
    val sourceUrl: String,
    val title: String,
    val uploader: String = "",
    val host: String = "",
    val durationMs: Long? = null,
)

data class LinkBatch(
    val source: BatchSource,
    val sourceUrl: String,
    val label: String,
    val title: String,
    val uploader: String = "",
    val entries: List<LinkBatchEntry>,
    val totalCount: Int,
    val returnedCount: Int,
    val isTruncated: Boolean,
)

data class BatchProgress(
    val total: Int,
    val processed: Int,
    val succeeded: Int,
    val failed: Int,
    val currentTitle: String? = null,
) {
    val isActive: Boolean get() = processed < total
}

data class LinkLiftUiState(
    val currentScreen: AppScreen = AppScreen.Home,
    val inputUrl: String = "",
    val preview: MediaPreview? = null,
    val selectedPreviewIndex: Int = 0,
    val selectedFormatIdsByItem: Map<Int, String> = emptyMap(),
    val showAudioOnly: Boolean = false,
    val selectedItemIndices: Set<Int> = emptySet(),
    val batch: LinkBatch? = null,
    val selectedBatchIndices: Set<Int> = emptySet(),
    val batchAudioOnly: Boolean = false,
    val batchProgress: BatchProgress? = null,
    val settings: UserPreferences = UserPreferences(),
    val targetSubfolder: String = "",
    val downloads: List<DownloadEntry> = emptyList(),
    val message: String? = null,
    val analysisStartedAt: Long? = null,
    val analysisLatencyMs: Long? = null,
    val downloadServiceAvailable: Boolean = true,
    val showYouTubeAuthPrompt: Boolean = false,
    val youTubeAuthPromptReason: String? = null,
    val isFromShare: Boolean = false,
)

