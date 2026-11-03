package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadPollingTest {

    private fun createDownloadEntry(id: Long, state: DownloadState): DownloadEntry =
        DownloadEntry(
            id = id,
            title = "Download $id",
            description = "Desc",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            state = state,
            progress = 0.5f,
            downloadedBytes = 500L,
            totalBytes = 1000L,
            localUri = null,
            subfolder = null,
            sourceUrl = "https://example.com/video/$id",
            updatedAt = System.currentTimeMillis(),
        )

    private fun createMergeJob(id: String, state: MergeJobState): MergeJobRecord =
        MergeJobRecord(
            id = id,
            title = "Merge $id",
            description = "Desc",
            sourceUrl = "https://example.com/video/$id",
            outputFileName = "$id.mp4",
            state = state,
            videoBytes = 500L,
            videoTotal = 1000L,
            audioBytes = 250L,
            audioTotal = 500L,
            resultUri = null,
            errorMessage = null,
            updatedAt = System.currentTimeMillis(),
            mimeType = "video/mp4",
        )

    @Test
    fun testPollingIntervalCalculation() {
        // Active downloads always poll at high frequency (2s)
        assertEquals(
            2_000L,
            LinkLiftViewModel.calculatePollingIntervalMs(hasActiveDownloads = true, isDownloadsScreenVisible = true),
        )
        assertEquals(
            2_000L,
            LinkLiftViewModel.calculatePollingIntervalMs(hasActiveDownloads = true, isDownloadsScreenVisible = false),
        )

        // When idle on Downloads screen, poll at lower frequency (8s)
        assertEquals(
            8_000L,
            LinkLiftViewModel.calculatePollingIntervalMs(hasActiveDownloads = false, isDownloadsScreenVisible = true),
        )

        // When idle on any other screen (Home, Settings, Preview), back off to 30s
        assertEquals(
            30_000L,
            LinkLiftViewModel.calculatePollingIntervalMs(hasActiveDownloads = false, isDownloadsScreenVisible = false),
        )
    }

    @Test
    fun testShouldPollDownloads() {
        // Should poll if downloads are active, regardless of current screen
        assertTrue(LinkLiftViewModel.shouldPollDownloads(hasActiveDownloads = true, isDownloadsScreenVisible = false))
        assertTrue(LinkLiftViewModel.shouldPollDownloads(hasActiveDownloads = true, isDownloadsScreenVisible = true))

        // Should poll if user is looking at Downloads screen, even if no active downloads
        assertTrue(LinkLiftViewModel.shouldPollDownloads(hasActiveDownloads = false, isDownloadsScreenVisible = true))

        // Must NOT poll when no downloads are running and user is not looking at Downloads screen
        assertFalse(LinkLiftViewModel.shouldPollDownloads(hasActiveDownloads = false, isDownloadsScreenVisible = false))
    }

    @Test
    fun testHasActiveDownloadsDetection() {
        // Empty state
        assertFalse(LinkLiftViewModel.hasActiveDownloads(emptyList(), emptyMap()))

        // Inactive system downloads
        val completedEntries = listOf(
            createDownloadEntry(1, DownloadState.Completed),
            createDownloadEntry(2, DownloadState.Failed),
        )
        assertFalse(LinkLiftViewModel.hasActiveDownloads(completedEntries, emptyMap()))

        // Active system downloads (Downloading or Queued)
        val downloadingEntries = listOf(
            createDownloadEntry(1, DownloadState.Completed),
            createDownloadEntry(2, DownloadState.Downloading),
        )
        assertTrue(LinkLiftViewModel.hasActiveDownloads(downloadingEntries, emptyMap()))

        val queuedEntries = listOf(
            createDownloadEntry(3, DownloadState.Queued),
        )
        assertTrue(LinkLiftViewModel.hasActiveDownloads(queuedEntries, emptyMap()))

        // Inactive merge jobs
        val inactiveMergeJobs = mapOf(
            "j1" to createMergeJob("j1", MergeJobState.Completed),
            "j2" to createMergeJob("j2", MergeJobState.Failed),
            "j3" to createMergeJob("j3", MergeJobState.Cancelled),
        )
        assertFalse(LinkLiftViewModel.hasActiveDownloads(emptyList(), inactiveMergeJobs))

        // Active merge jobs
        val activeStates = listOf(
            MergeJobState.Queued,
            MergeJobState.DownloadingVideo,
            MergeJobState.DownloadingAudio,
            MergeJobState.Muxing,
            MergeJobState.ConvertingAudio,
        )
        activeStates.forEach { state ->
            val activeJobs = mapOf("j-active" to createMergeJob("j-active", state))
            assertTrue(
                "Merge job with state $state must be detected as active",
                LinkLiftViewModel.hasActiveDownloads(emptyList(), activeJobs),
            )
        }
    }
}
