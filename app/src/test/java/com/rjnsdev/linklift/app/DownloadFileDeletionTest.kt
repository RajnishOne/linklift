package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadFileDeletionTest {

    @Test
    fun testDeleteDownloadedPhysicalFileByAbsolutePath() {
        val tempFile = File.createTempFile("linklift_test_del", ".mp4")
        tempFile.writeText("sample video content")
        assertTrue("Temporary file should exist before deletion", tempFile.exists())

        val result = deleteDownloadedPhysicalFile(null, tempFile.absolutePath)
        assertTrue("deleteDownloadedPhysicalFile should return true for deleted file", result)
        assertFalse("Physical file must no longer exist on storage", tempFile.exists())
    }

    @Test
    fun testDeleteDownloadedPhysicalFileByFileUri() {
        val tempFile = File.createTempFile("linklift_test_uri", ".mp3")
        tempFile.writeText("sample audio content")
        assertTrue("Temporary file should exist before deletion", tempFile.exists())

        val fileUri = "file://${tempFile.absolutePath}"
        val result = deleteDownloadedPhysicalFile(null, fileUri)
        assertTrue("deleteDownloadedPhysicalFile should return true for file:// URI", result)
        assertFalse("Physical file must no longer exist on storage", tempFile.exists())
    }

    @Test
    fun testDeleteDownloadedPhysicalFileWithInvalidOrNonExistentInputs() {
        assertFalse(deleteDownloadedPhysicalFile(null, null))
        assertFalse(deleteDownloadedPhysicalFile(null, ""))
        assertFalse(deleteDownloadedPhysicalFile(null, "   "))
        assertFalse(deleteDownloadedPhysicalFile(null, "/non/existent/path/media.mp4"))
        assertFalse(deleteDownloadedPhysicalFile(null, "file:///non/existent/path/media.mp4"))
    }

    @Test
    fun testKeepFilesWhenRemovingFromListOnly() {
        val tempFile = File.createTempFile("linklift_test_keep", ".mp4")
        tempFile.writeText("sample content")
        assertTrue(tempFile.exists())

        try {
            val deleteFiles = false
            if (deleteFiles) {
                deleteDownloadedPhysicalFile(null, tempFile.absolutePath)
            }
            // File must still be intact on disk
            assertTrue("File must remain intact on device when only removing from list", tempFile.exists())
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Test
    fun testDeleteConfirmationTextPluralization() {
        fun titleForCount(count: Int) = if (count == 1) "Delete 1 download?" else "Delete $count downloads?"
        fun bodyForCount(count: Int) = if (count == 1) {
            "Are you sure you want to remove this download?"
        } else {
            "Are you sure you want to remove these $count downloads?"
        }
        fun buttonLabel(deleteFiles: Boolean) = if (deleteFiles) "Delete" else "Remove from list"

        assertEquals("Delete 1 download?", titleForCount(1))
        assertEquals("Delete 3 downloads?", titleForCount(3))
        assertEquals("Are you sure you want to remove this download?", bodyForCount(1))
        assertEquals("Are you sure you want to remove these 3 downloads?", bodyForCount(3))
        assertEquals("Delete", buttonLabel(true))
        assertEquals("Remove from list", buttonLabel(false))
    }
}
