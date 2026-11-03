package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MergeJobInterruptedTest {

    @Before
    fun setUp() {
        MergeDownloadService.clearActiveJobsForTest()
        MergeJobStore.resetForTest()
    }

    private fun createJob(
        id: String,
        state: MergeJobState,
        errorMessage: String? = null,
        sourceUrl: String = "https://example.com/video/$id",
    ): MergeJobRecord = MergeJobRecord(
        id = id,
        title = "Job $id",
        description = "Test description",
        sourceUrl = sourceUrl,
        outputFileName = "$id.mp4",
        state = state,
        videoBytes = 1000L,
        videoTotal = 2000L,
        audioBytes = 500L,
        audioTotal = 1000L,
        resultUri = null,
        errorMessage = errorMessage,
        updatedAt = 1000L,
        mimeType = "video/mp4",
        subfolder = "MySubfolder",
    )

    @Test
    fun testAllActiveMergeJobsTransitionToFailedWhenOrphaned() {
        val activeStates = listOf(
            MergeJobState.Queued,
            MergeJobState.DownloadingVideo,
            MergeJobState.DownloadingAudio,
            MergeJobState.Muxing,
            MergeJobState.ConvertingAudio,
        )

        val initialJobs = activeStates.mapIndexed { index, state ->
            val id = "job-$index"
            id to createJob(id = id, state = state)
        }.toMap()

        val (reconciled, changed) = MergeJobStore.reconcileJobs(
            jobs = initialJobs,
            isWorkerActive = { false },
        )

        assertTrue("Expected changed to be true when orphaned jobs exist", changed)
        assertEquals(5, reconciled.size)

        reconciled.values.forEach { job ->
            assertEquals(
                "Job ${job.id} should have transitioned to Failed",
                MergeJobState.Failed,
                job.state,
            )
            assertEquals(
                "Job ${job.id} should have the interrupted error message",
                MergeJobStore.ERROR_DOWNLOAD_INTERRUPTED,
                job.errorMessage,
            )
            assertTrue("updatedAt should have been updated", job.updatedAt >= 1000L)
            assertEquals("MySubfolder", job.subfolder)
            assertEquals("https://example.com/video/${job.id}", job.sourceUrl)
        }
    }

    @Test
    fun testActiveMergeJobsRemainActiveWhenWorkerIsRunning() {
        val activeJobs = mapOf(
            "job-video" to createJob("job-video", MergeJobState.DownloadingVideo),
            "job-audio" to createJob("job-audio", MergeJobState.DownloadingAudio),
            "job-mux" to createJob("job-mux", MergeJobState.Muxing),
            "job-mp3" to createJob("job-mp3", MergeJobState.ConvertingAudio),
        )

        // All workers are active
        val (reconciled, changed) = MergeJobStore.reconcileJobs(
            jobs = activeJobs,
            isWorkerActive = { true },
        )

        assertFalse("Expected changed to be false when all workers are running", changed)
        assertEquals(MergeJobState.DownloadingVideo, reconciled["job-video"]?.state)
        assertEquals(MergeJobState.DownloadingAudio, reconciled["job-audio"]?.state)
        assertEquals(MergeJobState.Muxing, reconciled["job-mux"]?.state)
        assertEquals(MergeJobState.ConvertingAudio, reconciled["job-mp3"]?.state)
        assertNull(reconciled["job-video"]?.errorMessage)
    }

    @Test
    fun testSelectiveReconciliationMixedJobs() {
        val mixedJobs = mapOf(
            "job-active" to createJob("job-active", MergeJobState.DownloadingVideo),
            "job-orphaned" to createJob("job-orphaned", MergeJobState.ConvertingAudio),
            "job-completed" to createJob("job-completed", MergeJobState.Completed),
            "job-cancelled" to createJob("job-cancelled", MergeJobState.Cancelled, errorMessage = "Cancelled by user"),
            "job-failed" to createJob("job-failed", MergeJobState.Failed, errorMessage = "HTTP 403 Forbidden"),
        )

        val (reconciled, changed) = MergeJobStore.reconcileJobs(
            jobs = mixedJobs,
            isWorkerActive = { it == "job-active" },
        )

        assertTrue(changed)

        // Active job with running worker stays active
        assertEquals(MergeJobState.DownloadingVideo, reconciled["job-active"]?.state)
        assertNull(reconciled["job-active"]?.errorMessage)

        // Orphaned job without worker transitions to Failed
        assertEquals(MergeJobState.Failed, reconciled["job-orphaned"]?.state)
        assertEquals(MergeJobStore.ERROR_DOWNLOAD_INTERRUPTED, reconciled["job-orphaned"]?.errorMessage)

        // Terminal jobs remain untouched
        assertEquals(MergeJobState.Completed, reconciled["job-completed"]?.state)
        assertEquals(MergeJobState.Cancelled, reconciled["job-cancelled"]?.state)
        assertEquals("Cancelled by user", reconciled["job-cancelled"]?.errorMessage)
        assertEquals(MergeJobState.Failed, reconciled["job-failed"]?.state)
        assertEquals("HTTP 403 Forbidden", reconciled["job-failed"]?.errorMessage)
    }

    @Test
    fun testInterruptedJobMappingToDownloadEntryEnablesRetry() {
        val interruptedJob = createJob(
            id = "interrupted-1",
            state = MergeJobState.Failed,
            errorMessage = MergeJobStore.ERROR_DOWNLOAD_INTERRUPTED,
            sourceUrl = "https://www.youtube.com/watch?v=sample123",
        )

        val entry = interruptedJob.toDownloadEntry()

        assertEquals("DownloadState must be Failed to display retry button", DownloadState.Failed, entry.state)
        assertTrue(
            "Description should display the interrupted message",
            entry.description.contains(MergeJobStore.ERROR_DOWNLOAD_INTERRUPTED),
        )
        assertEquals("https://www.youtube.com/watch?v=sample123", entry.sourceUrl)
        assertEquals("MySubfolder", entry.subfolder)
    }

    @Test
    fun testMergeDownloadServiceActiveJobLifecycle() {
        assertFalse(MergeDownloadService.isJobActive("test-job-99"))

        MergeDownloadService.markJobActiveForTest("test-job-99")
        assertTrue(MergeDownloadService.isJobActive("test-job-99"))
        assertTrue(MergeDownloadService.getActiveJobIds().contains("test-job-99"))

        MergeDownloadService.clearActiveJobsForTest()
        assertFalse(MergeDownloadService.isJobActive("test-job-99"))
        assertTrue(MergeDownloadService.getActiveJobIds().isEmpty())
    }

    @Test
    fun testEmptyJobsReconciliation() {
        val (reconciled, changed) = MergeJobStore.reconcileJobs(
            jobs = emptyMap(),
            isWorkerActive = { false },
        )
        assertFalse(changed)
        assertTrue(reconciled.isEmpty())
    }

    @Test
    fun testReconciliationIdempotency() {
        val activeJob = createJob("idempotent-job", MergeJobState.DownloadingVideo)
        val initialMap = mapOf("idempotent-job" to activeJob)

        // First reconciliation: transitions to Failed
        val (firstRun, changed1) = MergeJobStore.reconcileJobs(
            jobs = initialMap,
            isWorkerActive = { false },
        )
        assertTrue(changed1)
        assertEquals(MergeJobState.Failed, firstRun["idempotent-job"]?.state)
        val firstUpdatedAt = firstRun["idempotent-job"]!!.updatedAt

        // Second reconciliation on already reconciled output: no-op, changed = false
        val (secondRun, changed2) = MergeJobStore.reconcileJobs(
            jobs = firstRun,
            isWorkerActive = { false },
        )
        assertFalse("Second reconciliation must not report changes", changed2)
        assertEquals(MergeJobState.Failed, secondRun["idempotent-job"]?.state)
        assertEquals(firstUpdatedAt, secondRun["idempotent-job"]?.updatedAt)
        assertEquals(MergeJobStore.ERROR_DOWNLOAD_INTERRUPTED, secondRun["idempotent-job"]?.errorMessage)
    }

    @Test
    fun testConcurrentJobsPreservedDuringIncrementalReconciliation() {
        // Simulate a running job and an orphaned job
        val orphaned = createJob("orphaned-job", MergeJobState.DownloadingVideo)
        val active = createJob("active-job", MergeJobState.DownloadingVideo)
        val jobsMap = mapOf("orphaned-job" to orphaned, "active-job" to active)

        val (reconciled, changed) = MergeJobStore.reconcileJobs(
            jobs = jobsMap,
            isWorkerActive = { it == "active-job" },
        )

        assertTrue(changed)
        // Orphaned transitioned to Failed
        assertEquals(MergeJobState.Failed, reconciled["orphaned-job"]?.state)
        assertEquals(MergeJobStore.ERROR_DOWNLOAD_INTERRUPTED, reconciled["orphaned-job"]?.errorMessage)

        // Active remains active and is never dropped
        assertEquals(MergeJobState.DownloadingVideo, reconciled["active-job"]?.state)
        assertNull(reconciled["active-job"]?.errorMessage)

        // Simulate concurrent addition of a third job
        val newJob = createJob("new-job", MergeJobState.Queued)
        val combined = reconciled + ("new-job" to newJob)
        assertEquals(3, combined.size)
        assertEquals(MergeJobState.Queued, combined["new-job"]?.state)
    }

    @Test
    fun testCleanupOrphanedJobFiles() {
        val tempDir = java.nio.file.Files.createTempDirectory("linklift_test_cleanup").toFile()
        try {
            val mergeDir = java.io.File(tempDir, "merge").apply { mkdirs() }
            val orphanedVideo = java.io.File(mergeDir, "job-orphan_video.mp4").apply { writeText("video data") }
            val orphanedAudio = java.io.File(mergeDir, "job-orphan_audio.m4a").apply { writeText("audio data") }
            val activeVideo = java.io.File(mergeDir, "job-active_video.mp4").apply { writeText("active data") }

            assertTrue(orphanedVideo.exists())
            assertTrue(orphanedAudio.exists())
            assertTrue(activeVideo.exists())

            MergeJobStore.cleanupOrphanedJobFiles(tempDir, listOf("job-orphan"))

            assertFalse("Orphaned video chunk must be deleted", orphanedVideo.exists())
            assertFalse("Orphaned audio chunk must be deleted", orphanedAudio.exists())
            assertTrue("Active job chunk must be preserved", activeVideo.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
