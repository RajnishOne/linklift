package com.rjnsdev.linklift.app

import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistRetrySubfolderTest {

    @Test
    fun testMergeJobFailedItemPreservesPlaylistSubfolderOnRetry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val playlistFolder = "Top Hits 2026"
        val jobId = "test-playlist-fail-retry-${System.currentTimeMillis()}"

        val record = MergeJobRecord(
            id = jobId,
            title = "Playlist Track 1",
            description = "High Quality Audio",
            sourceUrl = "https://example.com/playlist/song1",
            outputFileName = "Track1.mp3",
            state = MergeJobState.Failed,
            videoBytes = 0L,
            videoTotal = 0L,
            audioBytes = 0L,
            audioTotal = 1024L,
            resultUri = null,
            errorMessage = "Simulated network timeout",
            updatedAt = System.currentTimeMillis(),
            mimeType = "audio/mpeg",
            subfolder = playlistFolder,
        )

        // 1. Enqueue / upsert in MergeJobStore
        MergeJobStore.upsert(context, record)

        // Verify stored subfolder
        val enqueued = MergeJobStore.snapshot()[jobId]
        assertNotNull("Enqueued job must exist in snapshot", enqueued)
        assertEquals("Subfolder must match playlist folder", playlistFolder, enqueued?.subfolder)
        assertEquals(MergeJobState.Failed, enqueued?.state)

        // 2. Map to DownloadEntry for UI
        val failedEntry = enqueued!!.toDownloadEntry()
        assertEquals(DownloadState.Failed, failedEntry.state)
        assertEquals(playlistFolder, failedEntry.subfolder)

        // 3. Resolve retry subfolder using LinkLiftViewModel resolution hierarchy
        val resolvedSubfolder = failedEntry.subfolder?.takeIf { it.isNotBlank() }
            ?: (if (isMergeJobDownloadId(failedEntry.id)) {
                MergeJobStore.snapshot().values.firstOrNull { mergeJobToDownloadId(it.id) == failedEntry.id }?.subfolder
            } else null)?.takeIf { it.isNotBlank() }
            ?: MergeJobStore.snapshot().values.firstOrNull { it.sourceUrl == failedEntry.sourceUrl }?.subfolder?.takeIf { it.isNotBlank() }
            ?: extractSubfolderFromUriString(failedEntry.localUri)
            ?: ""

        assertEquals("Resolved subfolder must be playlist subfolder, NOT default root", playlistFolder, resolvedSubfolder)

        // 4. Clean up
        MergeJobStore.remove(context, setOf(jobId))
    }

    @Test
    fun testDataStoreTrackedSubfolderPersistenceForSystemDownloads() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val testDownloadId = 88776655L
            val testPlaylistFolder = "Rock Classics Album"
            val testSourceUrl = "https://example.com/rock/song2"

            // Store subfolder in DataStore as done during enqueuePreviewItems
            context.linkLiftDataStore.edit { prefs ->
                val subfoldersJson = prefs[PreferenceKeys.downloadSubfolders]
                val subfoldersMap = if (!subfoldersJson.isNullOrBlank()) {
                    val json = JSONObject(subfoldersJson)
                    buildMap<String, String> {
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            put(k, json.getString(k))
                        }
                    }.toMutableMap()
                } else {
                    mutableMapOf()
                }
                subfoldersMap[testDownloadId.toString()] = testPlaylistFolder
                val json = JSONObject()
                subfoldersMap.forEach { (k, v) -> json.put(k, v) }
                prefs[PreferenceKeys.downloadSubfolders] = json.toString()
            }

            // Read tracked subfolders
            val savedSubfolders = context.linkLiftDataStore.data
                .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
                .map { prefs ->
                    val raw = prefs[PreferenceKeys.downloadSubfolders]
                    if (raw.isNullOrBlank()) emptyMap<Long, String>()
                    else {
                        val json = JSONObject(raw)
                        buildMap<Long, String> {
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                k.toLongOrNull()?.let { id -> put(id, json.getString(k)) }
                            }
                        }
                    }
                }.first()

            assertEquals(testPlaylistFolder, savedSubfolders[testDownloadId])

            // Simulate retry resolution from DownloadEntry where subfolder was restored from DataStore
            val failedEntry = DownloadEntry(
                id = testDownloadId,
                title = "Rock Song",
                description = "Download failed (network reset)",
                mimeType = "audio/mpeg",
                kind = MediaKind.Audio,
                state = DownloadState.Failed,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = 2048L,
                localUri = null,
                sourceUrl = testSourceUrl,
                updatedAt = System.currentTimeMillis(),
                subfolder = savedSubfolders[testDownloadId],
            )

            assertEquals("Subfolder on DownloadEntry must match playlist folder", testPlaylistFolder, failedEntry.subfolder)
            val resolved = failedEntry.subfolder?.takeIf { it.isNotBlank() } ?: ""
            assertEquals(testPlaylistFolder, resolved)

            // Clean up DataStore
            context.linkLiftDataStore.edit { prefs ->
                val subfoldersJson = prefs[PreferenceKeys.downloadSubfolders]
                if (!subfoldersJson.isNullOrBlank()) {
                    val json = JSONObject(subfoldersJson)
                    json.remove(testDownloadId.toString())
                    prefs[PreferenceKeys.downloadSubfolders] = json.toString()
                }
            }
        }
    }

    @Test
    fun testUriStringSubfolderExtractionFallback() {
        val localUri = "file:///storage/emulated/0/Download/LinkLift/Jazz%20Festival%202026/track03.mp3"
        val extracted = extractSubfolderFromUriString(localUri)
        assertEquals("Jazz Festival 2026", extracted)

        // When DownloadEntry.subfolder was null, fallback to URI must extract the playlist folder
        val failedEntry = DownloadEntry(
            id = 554433L,
            title = "Jazz Track",
            description = "Download failed",
            mimeType = "audio/mpeg",
            kind = MediaKind.Audio,
            state = DownloadState.Failed,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = 1000L,
            localUri = localUri,
            sourceUrl = "https://example.com/jazz/3",
            updatedAt = System.currentTimeMillis(),
            subfolder = null,
        )

        val resolvedSubfolder = failedEntry.subfolder?.takeIf { it.isNotBlank() }
            ?: extractSubfolderFromUriString(failedEntry.localUri)
            ?: ""

        assertEquals("Jazz Festival 2026", resolvedSubfolder)
    }
}
