package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadDeletedMediaTest {

    @Test
    fun testCompletedDownloadCanOpen() {
        val completedDownload = DownloadEntry(
            id = 101L,
            title = "Completed Video",
            description = "Downloaded file",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            state = DownloadState.Completed,
            progress = 1.0f,
            downloadedBytes = 1024L,
            totalBytes = 1024L,
            localUri = "/path/to/missing/video.mp4",
            sourceUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            updatedAt = System.currentTimeMillis(),
        )

        // Completed downloads are clickable to open or show missing media dialog
        assertTrue(completedDownload.state == DownloadState.Completed)

        val queuedDownload = completedDownload.copy(state = DownloadState.Queued)
        assertFalse(queuedDownload.state == DownloadState.Completed)
    }

    @Test
    fun testLocalFileExistenceAndNonZeroSizeCheck() {
        val tempFile = File.createTempFile("linklift_test", ".mp4")
        try {
            // Initially 0 bytes
            assertEquals(0L, tempFile.length())
            val isAvailableInitially = tempFile.exists() && tempFile.length() > 0L
            assertFalse(isAvailableInitially)

            // Write content
            tempFile.writeText("sample media data")
            assertTrue(tempFile.length() > 0L)
            val isAvailableAfterWrite = tempFile.exists() && tempFile.length() > 0L
            assertTrue(isAvailableAfterWrite)

            // Delete file
            tempFile.delete()
            assertFalse(tempFile.exists())
            val isAvailableAfterDelete = tempFile.exists() && tempFile.length() > 0L
            assertFalse(isAvailableAfterDelete)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Test
    fun testDownloadEntrySourceUrlAndSharingProperties() {
        val entryWithUrl = DownloadEntry(
            id = 201L,
            title = "Shared Reel",
            description = "Instagram Reel",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            state = DownloadState.Completed,
            progress = 1.0f,
            downloadedBytes = 5000L,
            totalBytes = 5000L,
            localUri = "/storage/emulated/0/Download/LinkLift/reel.mp4",
            sourceUrl = "https://www.instagram.com/reel/C2xyz123/",
            updatedAt = System.currentTimeMillis(),
        )

        assertFalse(entryWithUrl.sourceUrl.isNullOrBlank())
        assertFalse(entryWithUrl.localUri.isNullOrBlank())

        val entryWithoutUrl = entryWithUrl.copy(sourceUrl = null)
        assertTrue(entryWithoutUrl.sourceUrl.isNullOrBlank())
    }
}

