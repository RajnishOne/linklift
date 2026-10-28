package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadLocationTest {

    @Test
    fun sanitizeSubfolder_removesIllegalCharsAndSlashes() {
        assertEquals("", MergeDownloadService.sanitizeSubfolder(null))
        assertEquals("", MergeDownloadService.sanitizeSubfolder("   "))
        assertEquals("", MergeDownloadService.sanitizeSubfolder("///"))
        assertEquals("Music", MergeDownloadService.sanitizeSubfolder("/Music/"))
        assertEquals("Math_Course", MergeDownloadService.sanitizeSubfolder("Math:Course"))
        assertEquals("My_Folder", MergeDownloadService.sanitizeSubfolder("My?*\"<>|Folder"))
        assertEquals("Folder/Sub", MergeDownloadService.sanitizeSubfolder("Folder/Sub"))
    }

    @Test
    fun userPreferences_effectiveDownloadPath() {
        val rootPrefs = UserPreferences(downloadSubfolder = "")
        assertEquals("Downloads/LinkLift", rootPrefs.effectiveDownloadPath)

        val customPrefs = UserPreferences(downloadSubfolder = "Music")
        assertEquals("Downloads/LinkLift/Music", customPrefs.effectiveDownloadPath)

        val trimmedPrefs = UserPreferences(downloadSubfolder = "  Study/Lectures  ")
        assertEquals("Downloads/LinkLift/Study/Lectures", trimmedPrefs.effectiveDownloadPath)
    }

    @Test
    fun mergeJobMapping_completedStatusReflectsSubfolder() {
        val rootRecord = MergeJobRecord(
            id = "test-1",
            title = "Test Video",
            description = "1080p",
            sourceUrl = "https://example.com/video",
            outputFileName = "test.mp4",
            state = MergeJobState.Completed,
            videoBytes = 100L,
            videoTotal = 100L,
            audioBytes = 50L,
            audioTotal = 50L,
            resultUri = "content://downloads/1",
            errorMessage = null,
            updatedAt = 1000L,
            mimeType = "video/mp4",
            subfolder = null,
        )
        val rootEntry = rootRecord.toDownloadEntry()
        assertEquals("1080p • Saved to Downloads/LinkLift", rootEntry.description)

        val subfolderRecord = rootRecord.copy(subfolder = "History")
        val subfolderEntry = subfolderRecord.toDownloadEntry()
        assertEquals("1080p • Saved to Downloads/LinkLift/History", subfolderEntry.description)
    }
}
