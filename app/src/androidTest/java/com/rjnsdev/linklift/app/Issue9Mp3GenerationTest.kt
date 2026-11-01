package com.rjnsdev.linklift.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.rjnsdev.linklift.app.util.AudioToMp3Converter
import com.rjnsdev.linklift.app.util.Mp3Encoder
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class Issue9Mp3GenerationTest {

    companion object {
        private const val TAG = "Issue9Mp3Test"
        private const val PLAYLIST_URL =
            "https://youtube.com/playlist?list=PL5NarVlMDfGH57UMh8X7VSKOu-SsQYGdg&si=0ufkxqZ9LyM1DtNt"
    }

    @Test
    fun testNativeLameLibraryLoaded() {
        Log.i(TAG, "Testing if native linklift-mp3 library is loaded...")
        val available = Mp3Encoder.isNativeAvailable()
        Log.i(TAG, "Mp3Encoder.isNativeAvailable: $available")
        assertTrue("Native LAME MP3 encoder library must be loaded", available)
    }

    @Test
    fun testPlaylistResolutionAndMp3GenerationOnDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.i(TAG, "Starting playlist resolution on device for: $PLAYLIST_URL")

        // 1. Initialize Chaquopy Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val python = Python.getInstance()
        val resolverModule = python.getModule("generic_media_resolver")

        // 2. Resolve Playlist
        val playlistPayloadStr = resolverModule.callAttr("resolve_playlist", PLAYLIST_URL, null, 10).toString()
        Log.i(TAG, "Playlist resolved payload: ${playlistPayloadStr.take(300)}...")
        val playlistJson = JSONObject(playlistPayloadStr)
        val isPlaylist = playlistJson.optBoolean("is_playlist", false)
        assertTrue("URL must be recognized as playlist", isPlaylist)

        val entries = playlistJson.optJSONArray("entries")
        assertNotNull("Playlist must contain entries", entries)
        assertTrue("Playlist entries must not be empty", entries!!.length() > 0)

        val firstEntry = entries.getJSONObject(0)
        val entryUrl = firstEntry.getString("source_url")
        val songTitle = firstEntry.optString("title", "Test Track")
        val uploader = firstEntry.optString("uploader", "")
        Log.i(TAG, "Processing first playlist item: '$songTitle' ($entryUrl) by '$uploader'")

        // 3. Resolve Media Formats for the entry
        val singleItemPayloadStr = resolverModule.callAttr("resolve_url", entryUrl, null).toString()
        val singleJson = JSONObject(singleItemPayloadStr)
        val itemsArray = singleJson.optJSONArray("items")
        val formatsArray = if (itemsArray != null && itemsArray.length() > 0) {
            itemsArray.getJSONObject(0).optJSONArray("formats")
        } else {
            singleJson.optJSONArray("formats")
        }
        assertNotNull("Should have formats array", formatsArray)
        assertTrue("Should have media formats available", formatsArray!!.length() > 0)

        val formatList = mutableListOf<MediaFormat>()
        for (i in 0 until formatsArray.length()) {
            val f = formatsArray.getJSONObject(i)
            val formatId = f.optString("format_id", i.toString())
            val label = f.optString("label", "Audio")
            val mediaUrl = f.optString("media_url").ifBlank { f.optString("url") }
            val mimeType = f.optString("mime_type", "")
            val ext = f.optString("ext", "m4a")
            val isAudioOnly = f.optBoolean("is_audio_only", false) || f.optString("kind") == "audio"
            val abr = f.optInt("abr", 0).takeIf { it > 0 }
            val tbr = f.optInt("tbr", 0).takeIf { it > 0 }
            val headersMap = mutableMapOf<String, String>()
            val httpHeaders = f.optJSONObject("http_headers")
            if (httpHeaders != null) {
                val keys = httpHeaders.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    headersMap[k] = httpHeaders.getString(k)
                }
            }

            if (mediaUrl.isNotBlank()) {
                formatList.add(
                    MediaFormat(
                        formatId = formatId,
                        label = label,
                        mediaUrl = mediaUrl,
                        mimeType = mimeType,
                        kind = if (isAudioOnly) MediaKind.Audio else MediaKind.Video,
                        ext = ext,
                        height = null,
                        width = null,
                        fps = null,
                        abr = abr,
                        tbr = tbr,
                        fileSizeBytes = f.optLong("filesize", 0L).takeIf { it > 0L },
                        fileSizeApprox = false,
                        isAudioOnly = isAudioOnly,
                        isProgressive = f.optBoolean("is_progressive", false),
                        isVideoOnly = f.optBoolean("is_video_only", false),
                        httpHeaders = headersMap,
                    )
                )
            }
        }

        Log.i(TAG, "Found ${formatList.size} formats total. Audio-only count: ${formatList.count { it.isAudioOnly }}")

        // 4. Choose format according to MP3 preference (per Issue 9)
        val chosenFormat = chooseFormatForPreset(
            formats = formatList,
            preset = QualityPreset.BestQuality,
            audioPref = AudioFormatPreference.Mp3,
            preferAudioOnly = true,
        )
        assertNotNull("Should select an audio format candidate for MP3 preference", chosenFormat)
        Log.i(
            TAG,
            "Chosen candidate format: id=${chosenFormat!!.formatId}, ext=${chosenFormat.ext}, mime=${chosenFormat.mimeType}, abr=${chosenFormat.abr}"
        )

        val routingMerge = shouldDownloadWithMergeService(
            format = chosenFormat,
            sourceUrl = entryUrl,
            audioPref = AudioFormatPreference.Mp3,
        )
        assertTrue("MP3 preference must route via MergeDownloadService transcoding pipeline", routingMerge)

        // 5. Download the raw audio stream to temporary cache
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val reqBuilder = Request.Builder().url(chosenFormat.mediaUrl)
        chosenFormat.httpHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
        val req = reqBuilder.build()

        val tempAudioInput = File(context.cacheDir, "issue9_input_${System.currentTimeMillis()}.${chosenFormat.ext}")
        Log.i(TAG, "Downloading raw audio stream from YouTube to ${tempAudioInput.name}...")
        client.newCall(req).execute().use { resp ->
            assertTrue("HTTP download should be successful (${resp.code})", resp.isSuccessful)
            val body = resp.body
            assertNotNull("Response body must not be null", body)
            tempAudioInput.outputStream().use { out ->
                body!!.byteStream().copyTo(out)
            }
        }
        val inputSize = tempAudioInput.length()
        Log.i(TAG, "Downloaded raw audio successfully! Size: $inputSize bytes")
        assertTrue("Input audio file must not be empty", inputSize > 0)

        // 6. Execute MP3 Generation Logic on Device (AudioToMp3Converter via MediaCodec + LAME)
        val outputMp3 = File(context.cacheDir, "issue9_converted_${System.currentTimeMillis()}.mp3")
        Log.i(TAG, "Starting AudioToMp3Converter transcoding to ${outputMp3.name}...")

        val startTime = System.currentTimeMillis()
        val convertSuccess = runBlocking {
            AudioToMp3Converter.convert(
                inputFile = tempAudioInput,
                outputFile = outputMp3,
                title = songTitle,
                artist = uploader,
                bitrateKbps = 192,
                onProgress = { progress ->
                    Log.d(TAG, "Transcoding progress: ${(progress * 100).toInt()}%")
                },
            )
        }
        val durationMs = System.currentTimeMillis() - startTime

        Log.i(
            TAG,
            "Transcoding finished in ${durationMs}ms. Success: $convertSuccess, MP3 size: ${outputMp3.length()} bytes"
        )
        assertTrue("AudioToMp3Converter must return true", convertSuccess)
        assertTrue("Output MP3 file must exist", outputMp3.exists())
        val mp3Size = outputMp3.length()
        assertTrue("Output MP3 file must not be empty", mp3Size > 0)

        // 7. Verify MP3 structure (ID3 header or MPEG sync word)
        val header = ByteArray(10)
        outputMp3.inputStream().use { it.read(header) }
        val hasId3 = header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()
        val hasMpegSync = (header[0].toInt() and 0xFF) == 0xFF && (header[1].toInt() and 0xE0) == 0xE0

        Log.i(
            TAG,
            "MP3 Header verification: hasID3v2=$hasId3, hasMpegSync=$hasMpegSync (header=[${header.joinToString { "0x%02X".format(it) }}])"
        )
        assertTrue("Output file must be a valid MP3 (contain ID3v2 header or MPEG sync word)", hasId3 || hasMpegSync)

        Log.i(TAG, "SUCCESS! The MP3 generation logic for YouTube playlist item has verified on device.")

        // Clean up
        tempAudioInput.delete()
        outputMp3.delete()
    }
}
