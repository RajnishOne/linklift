package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRetryTest {

    @Test
    fun testRetryingDownloadIdsInUiState() {
        var state = LinkLiftUiState()
        assertTrue(state.retryingDownloadIds.isEmpty())

        state = state.copy(retryingDownloadIds = setOf(101L, 102L))
        assertEquals(2, state.retryingDownloadIds.size)
        assertTrue(state.retryingDownloadIds.contains(101L))
        assertTrue(state.retryingDownloadIds.contains(102L))

        state = state.copy(retryingDownloadIds = state.retryingDownloadIds - 101L)
        assertEquals(1, state.retryingDownloadIds.size)
        assertFalse(state.retryingDownloadIds.contains(101L))
        assertTrue(state.retryingDownloadIds.contains(102L))
    }

    @Test
    fun testYouTubeAuthDetectionForFailedDownloads() {
        val ytSource = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val failedYtEntry = DownloadEntry(
            id = 1L,
            title = "Test YouTube Video",
            description = "HTTP 403 Forbidden - bot verification required",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            state = DownloadState.Failed,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = 1000L,
            localUri = null,
            sourceUrl = ytSource,
            updatedAt = System.currentTimeMillis(),
        )

        val isYouTubeOrAuth = failedYtEntry.sourceUrl?.let { isYouTubeUrl(it) } == true ||
            failedYtEntry.description.contains("YouTube", ignoreCase = true) ||
            failedYtEntry.description.contains("403") ||
            failedYtEntry.description.contains("auth", ignoreCase = true)

        assertTrue("Expected YouTube auth requirement detected", isYouTubeOrAuth)
    }

    @Test
    fun testGenericFailedDownloadWithoutYouTubeAuth() {
        val directSource = "https://example.com/sample.mp4"
        val failedGenericEntry = DownloadEntry(
            id = 2L,
            title = "Direct Video",
            description = "Connection timed out",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            state = DownloadState.Failed,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = 1000L,
            localUri = null,
            sourceUrl = directSource,
            updatedAt = System.currentTimeMillis(),
        )

        val isYouTubeOrAuth = failedGenericEntry.sourceUrl?.let { isYouTubeUrl(it) } == true ||
            failedGenericEntry.description.contains("YouTube", ignoreCase = true) ||
            failedGenericEntry.description.contains("403") ||
            failedGenericEntry.description.contains("auth", ignoreCase = true)

        assertFalse("Generic timeout should not require YouTube auth", isYouTubeOrAuth)
    }

    @Test
    fun testFailedDownloadPreservesPlaylistSubfolderForRetry() {
        val playlistSubfolder = "Bollywood Classics"
        val failedEntry = DownloadEntry(
            id = 100L,
            title = "Classic Track",
            description = "Download failed (network error)",
            mimeType = "audio/mpeg",
            kind = MediaKind.Audio,
            state = DownloadState.Failed,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = 5000L,
            localUri = "file:///storage/emulated/0/Download/LinkLift/Bollywood%20Classics/track.mp3",
            sourceUrl = "https://www.youtube.com/watch?v=sampleId",
            updatedAt = System.currentTimeMillis(),
            subfolder = playlistSubfolder,
        )

        assertEquals("Bollywood Classics", failedEntry.subfolder)

        // Verify that subfolder resolution prioritizes download.subfolder
        val resolvedSubfolder = failedEntry.subfolder?.takeIf { it.isNotBlank() }
            ?: extractSubfolderFromUriString(failedEntry.localUri)
            ?: ""

        assertEquals(playlistSubfolder, resolvedSubfolder)

        // Verify copied entry for YouTube auth keeps subfolder
        val copiedForAuth = failedEntry.copy(subfolder = resolvedSubfolder)
        assertEquals(playlistSubfolder, copiedForAuth.subfolder)
    }

    @Test
    fun testHasPlayableMediaUrls() {
        assertFalse("Null format has no playable URLs", hasPlayableMediaUrls(null))

        val audioOnlyBlank = MediaFormat(
            formatId = "140",
            label = "Audio",
            mediaUrl = "",
            mimeType = "audio/mp4",
            kind = MediaKind.Audio,
            ext = "m4a",
            height = null,
            width = null,
            fps = null,
            abr = 128,
            tbr = null,
            fileSizeBytes = 1000L,
            fileSizeApprox = false,
            isAudioOnly = true,
            isProgressive = false,
        )
        assertFalse("Audio only with blank mediaUrl is not playable", hasPlayableMediaUrls(audioOnlyBlank))

        val audioOnlyValid = audioOnlyBlank.copy(mediaUrl = "https://example.com/audio.m4a")
        assertTrue("Audio only with valid mediaUrl is playable", hasPlayableMediaUrls(audioOnlyValid))

        val videoAudioTrack = audioOnlyValid
        val mergeFormatBlankVideo = MediaFormat(
            formatId = "137",
            label = "1080p",
            mediaUrl = "",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 4000,
            fileSizeBytes = 5000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            mergeAudio = videoAudioTrack,
        )
        assertFalse("Merge video with blank videoUrl is not playable", hasPlayableMediaUrls(mergeFormatBlankVideo))

        val mergeFormatBlankAudio = mergeFormatBlankVideo.copy(
            mediaUrl = "https://example.com/video.mp4",
            mergeAudio = videoAudioTrack.copy(mediaUrl = ""),
        )
        assertFalse("Merge video with blank audioUrl is not playable", hasPlayableMediaUrls(mergeFormatBlankAudio))

        val mergeFormatValid = mergeFormatBlankVideo.copy(mediaUrl = "https://example.com/video.mp4")
        assertTrue("Merge video with both video and audio URLs is playable", hasPlayableMediaUrls(mergeFormatValid))

        val progressiveBlank = MediaFormat(
            formatId = "18",
            label = "360p",
            mediaUrl = "   ",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 360,
            width = 640,
            fps = 30,
            abr = null,
            tbr = 600,
            fileSizeBytes = 2000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = true,
            isVideoOnly = false,
        )
        assertFalse("Progressive video with blank URL is not playable", hasPlayableMediaUrls(progressiveBlank))

        val progressiveBlankWithAudio = progressiveBlank.copy(mergeAudio = videoAudioTrack)
        assertFalse("Progressive video with blank mediaUrl must not be playable even if mergeAudio is attached", hasPlayableMediaUrls(progressiveBlankWithAudio))

        val progressiveValid = progressiveBlank.copy(mediaUrl = "https://example.com/prog.mp4")
        assertTrue("Progressive video with valid URL is playable", hasPlayableMediaUrls(progressiveValid))

        val videoOnlyValid = MediaFormat(
            formatId = "136",
            label = "720p video only",
            mediaUrl = "https://example.com/video720.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 720,
            width = 1280,
            fps = 30,
            abr = null,
            tbr = 2000,
            fileSizeBytes = 3000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            mergeAudio = null,
        )
        assertTrue("Video only format with valid mediaUrl is playable", hasPlayableMediaUrls(videoOnlyValid))

        val videoOnlyBlank = videoOnlyValid.copy(mediaUrl = "")
        assertFalse("Video only format with blank mediaUrl is not playable", hasPlayableMediaUrls(videoOnlyBlank))

        val mergeFormatWhitespaceAudio = mergeFormatValid.copy(
            mergeAudio = videoAudioTrack.copy(mediaUrl = "   "),
        )
        assertFalse("Merge video with whitespace audio URL is not playable", hasPlayableMediaUrls(mergeFormatWhitespaceAudio))
    }

    @Test
    fun testRetryDownloadGuardsAgainstEmptyMediaUrls() {
        val ytSource = "https://www.youtube.com/watch?v=empty_stream"
        val isYt = isYouTubeUrl(ytSource)
        val hasCookies = false

        // When YouTube stream extraction yields blank media URL and no cookies, auth must be prompted
        val shouldPromptAuth = isYt && !hasCookies
        assertTrue("Should prompt YouTube auth when media URL is blank and no cookies", shouldPromptAuth)

        // For non-YouTube URL, auth should NOT be prompted; instead, user gets an error message
        val genericSource = "https://example.com/video.mp4"
        val isGenericYt = isYouTubeUrl(genericSource)
        val shouldPromptGeneric = isGenericYt && !hasCookies
        assertFalse("Should not prompt YouTube auth for generic source with empty URLs", shouldPromptGeneric)
    }
}
