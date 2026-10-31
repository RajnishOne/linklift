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
}
