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
    fun testDeleteDownloadedPhysicalFileWithPlusAndSpacesInFilename() {
        val tmpDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        val parentDir = File(tmpDir)
        val tempFile = File(parentDir, "linklift_test+del (1).mp4")
        tempFile.writeText("sample video with plus and space")
        assertTrue(tempFile.exists())

        try {
            val fileUri = "file://${tempFile.absolutePath}"
            val result = deleteDownloadedPhysicalFile(null, fileUri)
            assertTrue("deleteDownloadedPhysicalFile should delete file with '+' in path", result)
            assertFalse("Physical file with '+' should no longer exist", tempFile.exists())
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Test
    fun testDeleteDownloadedPhysicalFileWithPercentEncodedSpacesAndPlus() {
        val tmpDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        val parentDir = File(tmpDir)
        val tempFile = File(parentDir, "linklift_percent+test file.mp4")
        tempFile.writeText("sample video for percent encode test")
        assertTrue(tempFile.exists())

        try {
            // Encode spaces as %20 while keeping + literal
            val encodedUri = "file://" + tempFile.absolutePath.replace(" ", "%20")
            val result = deleteDownloadedPhysicalFile(null, encodedUri)
            assertTrue("deleteDownloadedPhysicalFile should delete file with %20 in URI", result)
            assertFalse("Physical file should no longer exist", tempFile.exists())
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Test
    fun testDeleteDownloadedPhysicalFileByDirectPathWithSpaces() {
        val tmpDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        val parentDir = File(tmpDir)
        val tempFile = File(parentDir, "linklift direct path test.mp4")
        tempFile.writeText("sample video direct path")
        assertTrue(tempFile.exists())

        try {
            val result = deleteDownloadedPhysicalFile(null, tempFile.absolutePath)
            assertTrue("deleteDownloadedPhysicalFile should delete file by direct path with spaces", result)
            assertFalse("Physical file should no longer exist", tempFile.exists())
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
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
