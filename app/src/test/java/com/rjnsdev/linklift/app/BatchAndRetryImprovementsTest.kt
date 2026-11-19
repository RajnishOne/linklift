package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchAndRetryImprovementsTest {

    @Test
    fun testBatchMaxItemsIs1000() {
        assertEquals("BATCH_MAX_ITEMS must allow up to 1000 items for large playlists", 1000, BATCH_MAX_ITEMS)
    }

    @Test
    fun testFileConflictBehaviorEnum() {
        assertEquals(FileConflictBehavior.Skip, FileConflictBehavior.fromStorageKey("skip"))
        assertEquals(FileConflictBehavior.Overwrite, FileConflictBehavior.fromStorageKey("overwrite"))
        assertEquals(FileConflictBehavior.Rename, FileConflictBehavior.fromStorageKey("rename"))
        assertEquals(FileConflictBehavior.Rename, FileConflictBehavior.fromStorageKey(null))
        assertEquals(FileConflictBehavior.Rename, FileConflictBehavior.fromStorageKey("unknown"))

        val prefs = UserPreferences()
        assertEquals(FileConflictBehavior.Rename, prefs.fileConflictBehavior)
        assertFalse(prefs.downloadSubtitles)
    }

    @Test
    fun testVttToSrtConversion() {
        val sampleVtt = """
            WEBVTT
            Kind: captions
            Language: en

            00:00:01.200 --> 00:00:04.500
            <c.colorE5E5E5>First lyric line</c>

            00:00:05.123 --> 00:00:08.456 line:0 position:50%
            <v Vocalist>Second lyric line</v>
        """.trimIndent()

        val srt = convertVttToSrt(sampleVtt)

        assertTrue(srt.contains("1\n00:00:01,200 --> 00:00:04,500\nFirst lyric line"))
        assertTrue(srt.contains("2\n00:00:05,123 --> 00:00:08,456\nSecond lyric line"))
        assertFalse("SRT should not contain WEBVTT header", srt.contains("WEBVTT"))
        assertFalse("SRT should strip <c> tags", srt.contains("<c.colorE5E5E5>"))
        assertFalse("SRT should strip <v> tags", srt.contains("<v Vocalist>"))
    }

    @Test
    fun testVttShortTimestampFormat() {
        val shortVtt = """
            WEBVTT

            01:15.500 --> 01:20.000
            Short timestamp subtitle
        """.trimIndent()

        val srt = convertVttToSrt(shortVtt)

        assertTrue(srt.contains("00:01:15,500 --> 00:01:20,000"))
        assertTrue(srt.contains("Short timestamp subtitle"))
    }

    @Test
    fun testFailedDownloadsFilteringForRetryAll() {
        val downloads = listOf(
            DownloadEntry(
                id = 1L,
                title = "Item 1",
                description = "Complete",
                mimeType = "video/mp4",
                kind = MediaKind.Video,
                state = DownloadState.Completed,
                progress = 1f,
                downloadedBytes = 1000L,
                totalBytes = 1000L,
                localUri = "/path/1.mp4",
                sourceUrl = "https://example.com/1",
                updatedAt = 100L,
            ),
            DownloadEntry(
                id = 2L,
                title = "Item 2",
                description = "Failed connection",
                mimeType = "video/mp4",
                kind = MediaKind.Video,
                state = DownloadState.Failed,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = 1000L,
                localUri = null,
                sourceUrl = "https://example.com/2",
                updatedAt = 200L,
            ),
            DownloadEntry(
                id = 3L,
                title = "Item 3",
                description = "Failed 403",
                mimeType = "audio/mpeg",
                kind = MediaKind.Audio,
                state = DownloadState.Failed,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = 2000L,
                localUri = null,
                sourceUrl = "https://example.com/3",
                updatedAt = 300L,
            ),
            DownloadEntry(
                id = 4L,
                title = "Item 4",
                description = "Failed but no source url",
                mimeType = "video/mp4",
                kind = MediaKind.Video,
                state = DownloadState.Failed,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = 1000L,
                localUri = null,
                sourceUrl = null,
                updatedAt = 400L,
            ),
        )

        val retryable = downloads.filter {
            it.state == DownloadState.Failed && !it.sourceUrl.isNullOrBlank()
        }

        assertEquals(2, retryable.size)
        assertEquals(listOf(2L, 3L), retryable.map { it.id })
    }

    @Test
    fun testPreviewMediaItemSubtitlesSupport() {
        val item = PreviewMediaItem(
            resolvedUrl = "https://example.com/stream.mp4",
            title = "Test Song",
            description = "Test description",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            fileSizeBytes = 5000L,
            durationMs = 180000L,
            resolution = "1080p",
            fileName = "Test_Song.mp4",
            subtitleUrl = "https://example.com/sub.vtt",
            subtitleExt = "vtt",
        )

        assertEquals("https://example.com/sub.vtt", item.subtitleUrl)
        assertEquals("vtt", item.subtitleExt)

        val defaultItem = PreviewMediaItem(
            resolvedUrl = "https://example.com/stream.mp4",
            title = "Test Song",
            description = "Test description",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            fileSizeBytes = 5000L,
            durationMs = 180000L,
            resolution = "1080p",
            fileName = "Test_Song.mp4",
        )
        assertNull(defaultItem.subtitleUrl)
        assertNull(defaultItem.subtitleExt)
    }
}

