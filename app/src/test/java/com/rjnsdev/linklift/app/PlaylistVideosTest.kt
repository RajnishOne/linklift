package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests checking all 5 videos from playlist PLPs-gzgoaa4c
 * (https://youtube.com/playlist?list=PLPs-gzgoaa4c&si=9_9EJOu2niDYbH2G).
 *
 * Videos in playlist:
 * 1. 8v0s64d1Sic: "Claude for Beginners: How to Actually Use It (Full Guide)"
 * 2. _i-MHQDaBR4: "How to Use Claude AI: Complete Beginner's Guide (Maya's First Week)"
 * 3. bwPCZhNGTIw: "Claude Tutorial for Beginners (Step by Step, Free Plan)" (Reported issue: IllegalArgumentException)
 * 4. 7HQ4FWVwAsk: "Full Claude Tutorial for Beginners - Become A Pro In Just 30 Minutes!"
 * 5. 2nQO0y1NvqU: "The Ultimate Beginner's Guide to Claude AI"
 */
class PlaylistVideosTest {

    private val audioM4a140 = MediaFormat(
        formatId = "140",
        label = "Audio 128 kbps M4A",
        mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=140&clen=25000000",
        mimeType = "audio/mp4",
        kind = MediaKind.Audio,
        ext = "m4a",
        height = null,
        width = null,
        fps = null,
        abr = 128,
        tbr = 128,
        fileSizeBytes = 25_000_000L,
        fileSizeApprox = false,
        isAudioOnly = true,
        isProgressive = false,
        isVideoOnly = false,
        acodec = "mp4a.40.2",
    )

    private val audioWebm251 = MediaFormat(
        formatId = "251",
        label = "Audio 130 kbps WebM",
        mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=251&clen=26000000",
        mimeType = "audio/webm",
        kind = MediaKind.Audio,
        ext = "webm",
        height = null,
        width = null,
        fps = null,
        abr = 130,
        tbr = 130,
        fileSizeBytes = 26_000_000L,
        fileSizeApprox = false,
        isAudioOnly = true,
        isProgressive = false,
        isVideoOnly = false,
        acodec = "opus",
    )

    // Video 3: bwPCZhNGTIw - "Claude Tutorial for Beginners (Step by Step, Free Plan)"
    // 4K stream is format 313 (VP9 WebM, 2.27 GB), max MP4 is format 137 (1080p AVC1, 278 MB)
    private val video3Formats = listOf(
        MediaFormat(
            formatId = "313",
            label = "2160p 4K WebM",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=313&clen=2275349610",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 2160,
            width = 3840,
            fps = 30,
            abr = null,
            tbr = 11500,
            fileSizeBytes = 2_275_349_610L, // > 2.14GB (exceeds Int.MAX_VALUE)
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "vp9",
            mergeAudio = audioWebm251,
        ),
        MediaFormat(
            formatId = "271",
            label = "1440p 2K WebM",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=271&clen=478545400",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 1440,
            width = 2560,
            fps = 30,
            abr = null,
            tbr = 5000,
            fileSizeBytes = 478_545_400L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "vp9",
            mergeAudio = audioWebm251,
        ),
        MediaFormat(
            formatId = "137",
            label = "1080p MP4",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=137&clen=278184144",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 3000,
            fileSizeBytes = 278_184_144L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "avc1.640028",
            mergeAudio = audioM4a140,
        ),
        MediaFormat(
            formatId = "248",
            label = "1080p WebM",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=248&clen=156724676",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 1800,
            fileSizeBytes = 156_724_676L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "vp9",
            mergeAudio = audioWebm251,
        ),
        MediaFormat(
            formatId = "22",
            label = "720p MP4",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=22&clen=85000000",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 720,
            width = 1280,
            fps = 30,
            abr = 192,
            tbr = 1200,
            fileSizeBytes = 85_000_000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = true,
            isVideoOnly = false,
            vcodec = "avc1.64001F",
            acodec = "mp4a.40.2",
            mergeAudio = null,
        ),
        audioM4a140,
        audioWebm251,
    )

    // Video 1: 8v0s64d1Sic - "Claude for Beginners: How to Actually Use It (Full Guide)"
    // Has 4K MP4 (format 401, AV01, 213MB) and 4K WebM (format 313, VP9, 420MB)
    private val video1Formats = listOf(
        MediaFormat(
            formatId = "401",
            label = "2160p 4K MP4",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=401",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 2160,
            width = 3840,
            fps = 30,
            abr = null,
            tbr = 12000,
            fileSizeBytes = 213_089_710L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "av01",
            mergeAudio = audioM4a140,
        ),
        MediaFormat(
            formatId = "313",
            label = "2160p 4K WebM",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=313",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 2160,
            width = 3840,
            fps = 30,
            abr = null,
            tbr = 14000,
            fileSizeBytes = 420_470_212L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "vp9",
            mergeAudio = audioWebm251,
        ),
        audioM4a140,
        audioWebm251,
    )

    // Video 2: _i-MHQDaBR4 - "How to Use Claude AI: Complete Beginner's Guide (Maya's First Week)"
    // Only 1080p MP4 exists (47MB)
    private val video2Formats = listOf(
        MediaFormat(
            formatId = "137",
            label = "1080p MP4",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=137",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 2500,
            fileSizeBytes = 46_999_958L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "avc1",
            mergeAudio = audioM4a140,
        ),
        audioM4a140,
    )

    // Video 4: 7HQ4FWVwAsk - "Full Claude Tutorial for Beginners - Become A Pro In Just 30 Minutes!"
    private val video4Formats = listOf(
        MediaFormat(
            formatId = "137",
            label = "1080p MP4",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=137",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 3100,
            fileSizeBytes = 274_828_046L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "avc1",
            mergeAudio = audioM4a140,
        ),
        MediaFormat(
            formatId = "248",
            label = "1080p WebM",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=248",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 3300,
            fileSizeBytes = 310_409_814L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "vp9",
            mergeAudio = audioWebm251,
        ),
        audioM4a140,
        audioWebm251,
    )

    // Video 5: 2nQO0y1NvqU - "The Ultimate Beginner's Guide to Claude AI"
    // Short 2:17 video, 4K WebM (81MB), 1080p MP4 (16MB)
    private val video5Formats = listOf(
        MediaFormat(
            formatId = "313",
            label = "2160p 4K WebM",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=313",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 2160,
            width = 3840,
            fps = 30,
            abr = null,
            tbr = 9500,
            fileSizeBytes = 81_750_279L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "vp9",
            mergeAudio = audioWebm251,
        ),
        MediaFormat(
            formatId = "137",
            label = "1080p MP4",
            mediaUrl = "https://rr1---sn-xxx.googlevideo.com/videoplayback?itag=137",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 2000,
            fileSizeBytes = 16_411_136L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "avc1",
            mergeAudio = audioM4a140,
        ),
        audioM4a140,
        audioWebm251,
    )

    @Test
    fun testAllPlaylistUrlsNormalizedAndStrippedCorrectly() {
        val playlistIds = listOf("8v0s64d1Sic", "_i-MHQDaBR4", "bwPCZhNGTIw", "7HQ4FWVwAsk", "2nQO0y1NvqU")
        for ((index, id) in playlistIds.withIndex()) {
            // Test bare ID normalization
            val fromId = normalizeBatchEntryUrl(id)
            assertEquals("https://www.youtube.com/watch?v=$id", fromId)

            // Test relative watch path normalization
            val fromRel = normalizeBatchEntryUrl("/watch?v=$id")
            assertEquals("https://www.youtube.com/watch?v=$id", fromRel)

            // Test stripping playlist query parameters (list, index)
            val rawPlaylistUrl = "https://www.youtube.com/watch?v=$id&list=PLPs-gzgoaa4c&index=${index + 1}"
            val stripped = stripPlaylistQuery(rawPlaylistUrl)
            assertEquals("https://www.youtube.com/watch?v=$id", stripped)
        }
    }

    @Test
    fun testSafeFileNamesForAllPlaylistVideoTitles() {
        val titles = mapOf(
            "8v0s64d1Sic" to "Claude for Beginners: How to Actually Use It (Full Guide)",
            "_i-MHQDaBR4" to "How to Use Claude AI: Complete Beginner's Guide (Maya's First Week)",
            "bwPCZhNGTIw" to "Claude Tutorial for Beginners (Step by Step, Free Plan)",
            "7HQ4FWVwAsk" to "Full Claude Tutorial for Beginners - Become A Pro In Just 30 Minutes!",
            "2nQO0y1NvqU" to "The Ultimate Beginner's Guide to Claude AI",
        )

        for ((_, title) in titles) {
            val fileNameMp4 = buildSafeFileName(title = title, ext = "mp4")
            val fileNameWebm = buildSafeFileName(title = title, ext = "webm")

            assertTrue("Filename must end with .mp4", fileNameMp4.endsWith(".mp4"))
            assertTrue("Filename must end with .webm", fileNameWebm.endsWith(".webm"))
            assertFalse("Filename must not contain colons", fileNameMp4.contains(":"))
            assertFalse("Filename must not contain question marks", fileNameMp4.contains("?"))
            assertFalse("Filename must not contain slashes", fileNameMp4.contains("/"))
            assertFalse("Filename must not contain backslashes", fileNameMp4.contains("\\"))
        }

        // Specifically test Video 3 title with parentheses and commas
        val video3FileName = buildSafeFileName(
            title = "Claude Tutorial for Beginners (Step by Step, Free Plan)",
            ext = "webm"
        )
        assertEquals("Claude Tutorial for Beginners (Step by Step, Free Plan).webm", video3FileName)
    }

    @Test
    fun testVideo3FormatSelection_whenUserPrefersMp4_choosesBestMp4() {
        // When user preference is MP4, BestQuality should pick the best available MP4 (1080p MP4)
        // rather than being forced into 4K WebM.
        val chosen = chooseFormatForPreset(
            formats = video3Formats,
            preset = QualityPreset.BestQuality,
            videoPref = VideoFormatPreference.Mp4,
        )
        assertNotNull(chosen)
        assertEquals("137", chosen?.formatId)
        assertEquals("mp4", chosen?.ext)
        assertEquals(1080, chosen?.height)
        assertNotNull(chosen?.mergeAudio)
        assertEquals("m4a", chosen?.mergeAudio?.ext)
    }

    @Test
    fun testVideo3FormatSelection_whenUserPrefersAnyOrWebm_chooses4KWebm() {
        // When preference is Any, BestQuality picks the highest resolution available (4K WebM)
        val chosenAny = chooseFormatForPreset(
            formats = video3Formats,
            preset = QualityPreset.BestQuality,
            videoPref = VideoFormatPreference.Any,
        )
        assertNotNull(chosenAny)
        assertEquals("313", chosenAny?.formatId)
        assertEquals("webm", chosenAny?.ext)
        assertEquals(2160, chosenAny?.height)
        assertEquals(2_275_349_610L, chosenAny?.fileSizeBytes)
        assertNotNull(chosenAny?.mergeAudio)
        assertEquals("webm", chosenAny?.mergeAudio?.ext)

        // When preference is Webm, BestQuality picks 4K WebM
        val chosenWebm = chooseFormatForPreset(
            formats = video3Formats,
            preset = QualityPreset.BestQuality,
            videoPref = VideoFormatPreference.Webm,
        )
        assertNotNull(chosenWebm)
        assertEquals("313", chosenWebm?.formatId)
        assertEquals("webm", chosenWebm?.ext)
    }

    @Test
    fun testVideo3FormatSelection_res1080pPreset() {
        // Res1080p preset should select 1080p MP4 when preference is MP4 or Any
        val chosenMp4 = chooseFormatForPreset(
            formats = video3Formats,
            preset = QualityPreset.Res1080p,
            videoPref = VideoFormatPreference.Mp4,
        )
        assertEquals("137", chosenMp4?.formatId)
        assertEquals("mp4", chosenMp4?.ext)

        val chosenAny = chooseFormatForPreset(
            formats = video3Formats,
            preset = QualityPreset.Res1080p,
            videoPref = VideoFormatPreference.Any,
        )
        assertEquals("137", chosenAny?.formatId)
        assertEquals("mp4", chosenAny?.ext)
    }

    @Test
    fun testAllVideosFormatSelection_bestQuality() {
        // Video 1: Has 4K MP4 and 4K WebM -> Any prefers MP4 at equal resolution
        val chosen1 = chooseFormatForPreset(video1Formats, QualityPreset.BestQuality, VideoFormatPreference.Any)
        assertEquals("401", chosen1?.formatId)
        assertEquals("mp4", chosen1?.ext)
        assertEquals(2160, chosen1?.height)

        // Video 2: Only 1080p MP4 -> returns 1080p MP4
        val chosen2 = chooseFormatForPreset(video2Formats, QualityPreset.BestQuality, VideoFormatPreference.Any)
        assertEquals("137", chosen2?.formatId)
        assertEquals("mp4", chosen2?.ext)

        // Video 4: Has 1080p MP4 and 1080p WebM -> prefers MP4
        val chosen4 = chooseFormatForPreset(video4Formats, QualityPreset.BestQuality, VideoFormatPreference.Any)
        assertEquals("137", chosen4?.formatId)
        assertEquals("mp4", chosen4?.ext)

        // Video 5: Has 4K WebM and 1080p MP4 -> Any picks 4K WebM
        val chosen5 = chooseFormatForPreset(video5Formats, QualityPreset.BestQuality, VideoFormatPreference.Any)
        assertEquals("313", chosen5?.formatId)
        assertEquals("webm", chosen5?.ext)
        assertEquals(2160, chosen5?.height)

        // Video 5: With MP4 pref -> picks 1080p MP4
        val chosen5Mp4 = chooseFormatForPreset(video5Formats, QualityPreset.BestQuality, VideoFormatPreference.Mp4)
        assertEquals("137", chosen5Mp4?.formatId)
        assertEquals("mp4", chosen5Mp4?.ext)
    }

    @Test
    fun testAudioOnlySelectionForAllVideos() {
        val allVideoFormats = listOf(video1Formats, video2Formats, video3Formats, video4Formats, video5Formats)
        for (formats in allVideoFormats) {
            val chosen = chooseFormatForPreset(
                formats = formats,
                preset = QualityPreset.BestQuality,
                preferAudioOnly = true,
                audioPref = AudioFormatPreference.M4a,
            )
            assertNotNull(chosen)
            assertTrue("Must be audio-only", chosen?.isAudioOnly == true)
            assertEquals("m4a", chosen?.ext)
        }
    }

    @Test
    fun testAudioPairCompatibilityForAllVideos() {
        val allVideoFormats = listOf(video1Formats, video2Formats, video3Formats, video4Formats, video5Formats)
        for (formats in allVideoFormats) {
            for (fmt in formats.filter { it.isVideoOnly }) {
                val pair = fmt.mergeAudio
                assertNotNull("Video-only stream ${fmt.formatId} (${fmt.ext}) must have audio pair", pair)
                assertTrue("Audio pair must be audio-only", pair!!.isAudioOnly)
                if (fmt.ext.equals("mp4", ignoreCase = true)) {
                    assertTrue("MP4 video must pair with M4A/AAC audio", pair.ext.equals("m4a", ignoreCase = true) || pair.ext.equals("aac", ignoreCase = true))
                } else if (fmt.ext.equals("webm", ignoreCase = true)) {
                    assertTrue("WebM video must pair with WebM/Opus audio", pair.ext.equals("webm", ignoreCase = true) || pair.ext.equals("opus", ignoreCase = true))
                }
            }
        }
    }

    @Test
    fun testLinkBatchEntryStructureForAllPlaylistVideos() {
        val entries = listOf(
            LinkBatchEntry(
                sourceUrl = "https://www.youtube.com/watch?v=8v0s64d1Sic",
                title = "Claude for Beginners: How to Actually Use It (Full Guide)",
                uploader = "AI Guides",
                host = "youtube.com",
                durationMs = 1512000L,
            ),
            LinkBatchEntry(
                sourceUrl = "https://www.youtube.com/watch?v=_i-MHQDaBR4",
                title = "How to Use Claude AI: Complete Beginner's Guide (Maya's First Week)",
                uploader = "AI Guides",
                host = "youtube.com",
                durationMs = 422000L,
            ),
            LinkBatchEntry(
                sourceUrl = "https://www.youtube.com/watch?v=bwPCZhNGTIw",
                title = "Claude Tutorial for Beginners (Step by Step, Free Plan)",
                uploader = "AI Guides",
                host = "youtube.com",
                durationMs = 1569000L,
            ),
            LinkBatchEntry(
                sourceUrl = "https://www.youtube.com/watch?v=7HQ4FWVwAsk",
                title = "Full Claude Tutorial for Beginners - Become A Pro In Just 30 Minutes!",
                uploader = "AI Guides",
                host = "youtube.com",
                durationMs = 1915000L,
            ),
            LinkBatchEntry(
                sourceUrl = "https://www.youtube.com/watch?v=2nQO0y1NvqU",
                title = "The Ultimate Beginner's Guide to Claude AI",
                uploader = "AI Guides",
                host = "youtube.com",
                durationMs = 137000L,
            ),
        )

        val batch = LinkBatch(
            source = BatchSource.Playlist,
            sourceUrl = "https://youtube.com/playlist?list=PLPs-gzgoaa4c&si=9_9EJOu2niDYbH2G",
            label = "YouTube playlist",
            title = "Claude Tutorials",
            uploader = "AI Guides",
            entries = entries,
            totalCount = 5,
            returnedCount = 5,
            isTruncated = false,
        )

        assertEquals(5, batch.entries.size)
        assertEquals(5, batch.totalCount)
        assertFalse(batch.isTruncated)
        assertEquals("Claude Tutorial for Beginners (Step by Step, Free Plan)", batch.entries[2].title)
        assertEquals("https://www.youtube.com/watch?v=bwPCZhNGTIw", batch.entries[2].sourceUrl)
    }
}
