package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatPreferenceTest {

    @Test
    fun qualityPreset_fromStorageKey_resolvesProperly() {
        assertEquals(QualityPreset.BestQuality, QualityPreset.fromStorageKey("best_quality"))
        assertEquals(QualityPreset.Res1080p, QualityPreset.fromStorageKey("1080p"))
        assertEquals(QualityPreset.Res720p, QualityPreset.fromStorageKey("720p"))
        assertEquals(QualityPreset.Res480p, QualityPreset.fromStorageKey("480p"))
        assertEquals(QualityPreset.Res360p, QualityPreset.fromStorageKey("360p"))
        assertEquals(QualityPreset.SmallestFile, QualityPreset.fromStorageKey("smallest_file"))
        assertEquals(QualityPreset.AskEveryTime, QualityPreset.fromStorageKey("ask_every_time"))
        assertEquals(QualityPreset.BestQuality, QualityPreset.fromStorageKey("unknown_key"))
        assertEquals(QualityPreset.BestQuality, QualityPreset.fromStorageKey(null))
    }

    @Test
    fun videoFormatPreference_fromStorageKey_resolvesProperly() {
        assertEquals(VideoFormatPreference.Any, VideoFormatPreference.fromStorageKey("any"))
        assertEquals(VideoFormatPreference.Mp4, VideoFormatPreference.fromStorageKey("mp4"))
        assertEquals(VideoFormatPreference.Webm, VideoFormatPreference.fromStorageKey("webm"))
        assertEquals(VideoFormatPreference.Any, VideoFormatPreference.fromStorageKey("unknown"))
        assertEquals(VideoFormatPreference.Any, VideoFormatPreference.fromStorageKey(null))
    }

    @Test
    fun audioFormatPreference_fromStorageKey_resolvesProperly() {
        assertEquals(AudioFormatPreference.Any, AudioFormatPreference.fromStorageKey("any"))
        assertEquals(AudioFormatPreference.M4a, AudioFormatPreference.fromStorageKey("m4a"))
        assertEquals(AudioFormatPreference.Opus, AudioFormatPreference.fromStorageKey("opus"))
        assertEquals(AudioFormatPreference.Mp3, AudioFormatPreference.fromStorageKey("mp3"))
        assertEquals(AudioFormatPreference.Any, AudioFormatPreference.fromStorageKey("invalid"))
        assertEquals(AudioFormatPreference.Any, AudioFormatPreference.fromStorageKey(null))
    }

    @Test
    fun userPreferences_defaultFormats() {
        val prefs = UserPreferences()
        assertEquals(QualityPreset.BestQuality, prefs.preferredQuality)
        assertEquals(VideoFormatPreference.Any, prefs.preferredVideoFormat)
        assertEquals(AudioFormatPreference.Any, prefs.preferredAudioFormat)
    }

    @Test
    fun chooseFormatForPreset_preferAudioOnly_selectsAudioFormat() {
        val videoFormat = MediaFormat(
            formatId = "18",
            label = "360p MP4",
            mediaUrl = "https://example.com/video.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 360,
            width = 640,
            fps = 30,
            abr = null,
            tbr = 800,
            fileSizeBytes = 10000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = true,
            isVideoOnly = false,
        )
        val audioFormat = MediaFormat(
            formatId = "140",
            label = "Audio 128kbps M4A",
            mediaUrl = "https://example.com/audio.m4a",
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
            isVideoOnly = false,
        )
        val formats = listOf(videoFormat, audioFormat)

        // When preferAudioOnly = true, audio stream must be picked
        val chosenAudio = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.BestQuality,
            preferAudioOnly = true,
        )
        assertEquals("140", chosenAudio?.formatId)
        assertEquals(true, chosenAudio?.isAudioOnly)

        // When preferAudioOnly = false, video stream must be picked
        val chosenVideo = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.BestQuality,
            preferAudioOnly = false,
        )
        assertEquals("18", chosenVideo?.formatId)
        assertEquals(false, chosenVideo?.isAudioOnly)
    }

    @Test
    fun chooseFormatForPreset_preferAudioOnly_respectsAudioFormatPreference() {
        val audioM4a = MediaFormat(
            formatId = "140",
            label = "Audio 128kbps M4A",
            mediaUrl = "https://example.com/audio.m4a",
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
            isVideoOnly = false,
        )
        val audioOpus = MediaFormat(
            formatId = "251",
            label = "Audio 160kbps Opus",
            mediaUrl = "https://example.com/audio.opus",
            mimeType = "audio/webm",
            kind = MediaKind.Audio,
            ext = "opus",
            height = null,
            width = null,
            fps = null,
            abr = 160,
            tbr = 160,
            fileSizeBytes = 3500000L,
            fileSizeApprox = false,
            isAudioOnly = true,
            isProgressive = false,
            isVideoOnly = false,
        )
        val formats = listOf(audioM4a, audioOpus)

        val chosenM4a = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.BestQuality,
            audioPref = AudioFormatPreference.M4a,
            preferAudioOnly = true,
        )
        assertEquals("140", chosenM4a?.formatId)

        val chosenOpus = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.BestQuality,
            audioPref = AudioFormatPreference.Opus,
            preferAudioOnly = true,
        )
        assertEquals("251", chosenOpus?.formatId)
    }

    @Test
    fun chooseFormatForPreset_askEveryTime_withPreferAudioOnly() {
        val videoFormat = MediaFormat(
            formatId = "18",
            label = "360p MP4",
            mediaUrl = "https://example.com/video.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 360,
            width = 640,
            fps = 30,
            abr = null,
            tbr = 800,
            fileSizeBytes = 10000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = true,
            isVideoOnly = false,
        )
        val audioFormat = MediaFormat(
            formatId = "140",
            label = "Audio 128kbps M4A",
            mediaUrl = "https://example.com/audio.m4a",
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
            isVideoOnly = false,
        )
        val formats = listOf(videoFormat, audioFormat)

        val chosenAudio = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.AskEveryTime,
            preferAudioOnly = true,
        )
        assertEquals("140", chosenAudio?.formatId)
        assertEquals(true, chosenAudio?.isAudioOnly)

        val chosenVideo = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.AskEveryTime,
            preferAudioOnly = false,
        )
        assertEquals("18", chosenVideo?.formatId)
        assertEquals(false, chosenVideo?.isAudioOnly)
    }

    @Test
    fun chooseFormatForPreset_bestQuality_prefersMp4OverWebm_whenAnyPreference() {
        val webm4k = MediaFormat(
            formatId = "313",
            label = "2160p WebM",
            mediaUrl = "https://example.com/2160p.webm",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 2160,
            width = 3840,
            fps = 30,
            abr = null,
            tbr = 10000, // Higher tbr than MP4
            fileSizeBytes = 50000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
        )
        val mp44k = MediaFormat(
            formatId = "401",
            label = "2160p MP4",
            mediaUrl = "https://example.com/2160p.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 2160,
            width = 3840,
            fps = 30,
            abr = null,
            tbr = 4500, // Lower tbr than WebM
            fileSizeBytes = 25000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
        )
        val formats = listOf(webm4k, mp44k)

        val chosenAny = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.BestQuality,
            videoPref = VideoFormatPreference.Any,
        )
        assertEquals("401", chosenAny?.formatId)
        assertEquals("mp4", chosenAny?.ext)

        val chosenWebm = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.BestQuality,
            videoPref = VideoFormatPreference.Webm,
        )
        assertEquals("313", chosenWebm?.formatId)
        assertEquals("webm", chosenWebm?.ext)
    }

    @Test
    fun chooseFormatForPreset_resolutionPreset_prefersMp4OverWebm_whenAnyPreference() {
        val webm720 = MediaFormat(
            formatId = "247",
            label = "720p WebM",
            mediaUrl = "https://example.com/720p.webm",
            mimeType = "video/webm",
            kind = MediaKind.Video,
            ext = "webm",
            height = 720,
            width = 1280,
            fps = 30,
            abr = null,
            tbr = 850, // Higher tbr than MP4
            fileSizeBytes = 10000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
        )
        val mp4720 = MediaFormat(
            formatId = "136",
            label = "720p MP4",
            mediaUrl = "https://example.com/720p.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.Video,
            ext = "mp4",
            height = 720,
            width = 1280,
            fps = 30,
            abr = null,
            tbr = 550, // Lower tbr than WebM
            fileSizeBytes = 7000000L,
            fileSizeApprox = false,
            isAudioOnly = false,
            isProgressive = false,
            isVideoOnly = true,
        )
        val formats = listOf(webm720, mp4720)

        val chosen = chooseFormatForPreset(
            formats = formats,
            preset = QualityPreset.Res720p,
            videoPref = VideoFormatPreference.Any,
        )
        assertEquals("136", chosen?.formatId)
        assertEquals("mp4", chosen?.ext)
    }
}
