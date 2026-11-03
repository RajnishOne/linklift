package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying stream format structure, MediaMuxer merge requirements,
 * and QualityPreset mapping.
 */
class MediaFormatMergeTest {

    @Test
    fun testRequiresMergeLogic() {
        val audioPair = MediaFormat(
            formatId = "140",
            label = "Audio 128kbps M4A",
            mediaUrl = "https://googlevideo.com/audio.m4a",
            mimeType = "audio/mp4",
            kind = MediaKind.Audio,
            ext = "m4a",
            height = null,
            width = null,
            fps = null,
            abr = 128,
            tbr = 128,
            fileSizeBytes = 3000000L,
            fileSizeApprox = false,
            isAudioOnly = true,
            isProgressive = false,
            isVideoOnly = false
        )

        // 1080p Video-only stream WITH audio pair -> requiresMerge = true
        val videoOnlyWithAudio = MediaFormat(
            formatId = "137",
            label = "1080p MP4",
            mediaUrl = "https://googlevideo.com/video1080p.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 4000,
            fileSizeBytes = 25000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "avc1",
            acodec = null,
            mergeAudio = audioPair
        )

        assertTrue("Video-only stream with paired audio must require merge", videoOnlyWithAudio.requiresMerge)

        // Progressive 720p stream (has both video & audio built-in) -> requiresMerge = false
        val progressiveVideo = MediaFormat(
            formatId = "22",
            label = "720p MP4",
            mediaUrl = "https://googlevideo.com/video720p.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 720,
            width = 1280,
            fps = 30,
            abr = 192,
            tbr = 2000,
            fileSizeBytes = 15000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = true,
            isVideoOnly = false,
            vcodec = "avc1",
            acodec = "mp4a",
            mergeAudio = null
        )

        assertFalse("Progressive stream with video and audio built-in does not require merge", progressiveVideo.requiresMerge)

        // Pure audio-only format -> requiresMerge = false
        assertFalse("Audio-only format does not require merge", audioPair.requiresMerge)
    }

    @Test
    fun testShouldDownloadWithMergeServiceForVideoOnly() {
        val audioPair = MediaFormat(
            formatId = "140",
            label = "Audio 128kbps",
            mediaUrl = "https://googlevideo.com/audio.m4a",
            mimeType = "audio/mp4",
            kind = MediaKind.Audio,
            ext = "m4a",
            height = null,
            width = null,
            fps = null,
            abr = 128,
            tbr = 128,
            fileSizeBytes = 3000000L,
            fileSizeApprox = false,
            isAudioOnly = true,
            isProgressive = false,
            isVideoOnly = false
        )

        val videoOnlyFormat = MediaFormat(
            formatId = "137",
            label = "1080p MP4",
            mediaUrl = "https://googlevideo.com/video1080p.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 1080,
            width = 1920,
            fps = 30,
            abr = null,
            tbr = 4000,
            fileSizeBytes = 25000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
            vcodec = "avc1",
            acodec = null,
            mergeAudio = audioPair
        )

        // Even for YouTube standard links, if requiresMerge=true, shouldDownloadWithMergeService must be true
        assertTrue(
            "Video-only format with audio pairing must route to MergeDownloadService",
            shouldDownloadWithMergeService(videoOnlyFormat, "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun testQualityPresetStorageKeys() {
        assertEquals(QualityPreset.BestQuality, QualityPreset.fromStorageKey("best_quality"))
        assertEquals(QualityPreset.SmallestFile, QualityPreset.fromStorageKey("smallest_file"))
        assertEquals(QualityPreset.AskEveryTime, QualityPreset.fromStorageKey("ask_every_time"))

        // Null or unknown key defaults to BestQuality
        assertEquals(QualityPreset.BestQuality, QualityPreset.fromStorageKey(null))
        assertEquals(QualityPreset.BestQuality, QualityPreset.fromStorageKey("unknown_preset_key"))
    }

    @Test
    fun testBuildSafeFileNameFromTitles() {
        // YouTube video title with special characters
        val ytTitle = "Rick Astley - Never Gonna Give You Up (Official Video) [4K / 60fps]?"
        val ytFileName = buildSafeFileName(
            title = ytTitle,
            ext = "mp4",
            fallbackUrl = "https://rr1---sn-abc.googlevideo.com/videoplayback?id=123",
            mimeType = "video/mp4"
        )
        assertEquals("Rick Astley - Never Gonna Give You Up (Official Video) [4K _ 60fps].mp4", ytFileName)

        // Instagram reel title / caption
        val igTitle = "Amazing Sunset in Bali! \uD83C\uDF05"
        val igFileName = buildSafeFileName(
            title = igTitle,
            ext = "mp4",
            fallbackUrl = "https://scontent.cdninstagram.com/v/t51/AQP2WUmTpYLQ12345.mp4?_nc_cat=100",
            mimeType = "video/mp4"
        )
        assertTrue(igFileName.startsWith("Amazing Sunset in Bali!"))
        assertTrue(igFileName.endsWith(".mp4"))
        assertFalse(igFileName.contains("AQP2WUmTpYLQ"))

        // Multi-item / carousel naming
        val carouselItem1 = multiItemFileName("Amazing Sunset in Bali.jpg", 0)
        val carouselItem2 = multiItemFileName("Amazing Sunset in Bali.jpg", 1)
        assertEquals("Amazing Sunset in Bali_1.jpg", carouselItem1)
        assertEquals("Amazing Sunset in Bali_2.jpg", carouselItem2)

        // Fallback when title is empty or generic
        val fallbackFileName = buildSafeFileName(
            title = "videoplayback",
            ext = "mp4",
            fallbackUrl = "https://example.com/custom_clip.mp4",
            mimeType = "video/mp4"
        )
        assertEquals("custom_clip.mp4", fallbackFileName)
    }

    @Test
    fun testBFramePresentationTimestampsPreserved() {
        // Decode order (DTS): I(0), P(66000), B(33000), B(49500), P(132000), B(99000)
        val decodeOrderRawPts = listOf(0L, 66_000L, 33_000L, 49_500L, 132_000L, 99_000L)
        val calculatedVideoPts = decodeOrderRawPts.map { raw ->
            MergeDownloadService.calculateSamplePts(raw, isAudio = false, firstPtsOffset = 0L)
        }

        // B-frame PTS must be preserved exactly in decode order for container presentation
        assertEquals(0L, calculatedVideoPts[0])
        assertEquals(66_000L, calculatedVideoPts[1])
        assertEquals(33_000L, calculatedVideoPts[2])
        assertEquals(49_500L, calculatedVideoPts[3])
        assertEquals(132_000L, calculatedVideoPts[4])
        assertEquals(99_000L, calculatedVideoPts[5])

        // Verify audio enforces strictly monotonic timestamps
        val audioRawPts = listOf(0L, 23_000L, 23_000L, 20_000L, 46_000L)
        var lastAudioPts = -1L
        val calculatedAudioPts = audioRawPts.map { raw ->
            val pts = MergeDownloadService.calculateSamplePts(raw, isAudio = true, lastAudioPts = lastAudioPts)
            lastAudioPts = pts
            pts
        }
        assertEquals(0L, calculatedAudioPts[0])
        assertEquals(23_000L, calculatedAudioPts[1])
        assertEquals(23_001L, calculatedAudioPts[2])
        assertEquals(23_002L, calculatedAudioPts[3])
        assertEquals(46_000L, calculatedAudioPts[4])
    }
}

