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
}
