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
        assertEquals("Download/LinkLift", rootPrefs.effectiveDownloadPath)

        val customPrefs = UserPreferences(downloadSubfolder = "Music")
        assertEquals("Download/LinkLift/Music", customPrefs.effectiveDownloadPath)

        val trimmedPrefs = UserPreferences(downloadSubfolder = "  Study/Lectures  ")
        assertEquals("Download/LinkLift/Study/Lectures", trimmedPrefs.effectiveDownloadPath)
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
        assertEquals("1080p • Saved to Download/LinkLift", rootEntry.description)
        assertEquals(null, rootEntry.subfolder)

        val subfolderRecord = rootRecord.copy(subfolder = "History")
        val subfolderEntry = subfolderRecord.toDownloadEntry()
        assertEquals("1080p • Saved to Download/LinkLift/History", subfolderEntry.description)
        assertEquals("History", subfolderEntry.subfolder)
    }

    @Test
    fun extractSubfolderFromUriString_correctlyParsesSubfolders() {
        assertEquals(
            "Bollywood Hits",
            extractSubfolderFromUriString("file:///storage/emulated/0/Download/LinkLift/Bollywood%20Hits/song.mp3")
        )
        assertEquals(
            "Bollywood Hits",
            extractSubfolderFromUriString("/storage/emulated/0/Download/LinkLift/Bollywood Hits/song.mp3")
        )
        assertEquals(
            "Playlists/Rock",
            extractSubfolderFromUriString("file:///storage/emulated/0/Download/LinkLift/Playlists/Rock/song.mp3")
        )
        assertEquals(
            null,
            extractSubfolderFromUriString("file:///storage/emulated/0/Download/LinkLift/song.mp3")
        )
        assertEquals(
            null,
            extractSubfolderFromUriString("content://media/external/downloads/1234")
        )
        assertEquals(
            null,
            extractSubfolderFromUriString(null)
        )
    }
}
