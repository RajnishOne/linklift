package com.rjnsdev.linklift.app.util

import com.rjnsdev.linklift.app.MediaFormat
import com.rjnsdev.linklift.app.MediaKind
import com.rjnsdev.linklift.app.buildGoogleVideoChunkUrl
import com.rjnsdev.linklift.app.extractAllUrls
import com.rjnsdev.linklift.app.extractFirstUrl
import com.rjnsdev.linklift.app.isExplicitPlaylistUrl
import com.rjnsdev.linklift.app.isInstagramUrl
import com.rjnsdev.linklift.app.isSupportedMediaUrl
import com.rjnsdev.linklift.app.isYouTubeAuthRequired
import com.rjnsdev.linklift.app.isYouTubeUrl
import com.rjnsdev.linklift.app.isYtDlpSupportedHost
import com.rjnsdev.linklift.app.looksLikePlaylistUrl
import com.rjnsdev.linklift.app.normalizeBatchEntryUrl
import com.rjnsdev.linklift.app.shouldDownloadWithMergeService
import com.rjnsdev.linklift.app.shouldRefreshUrlBeforeDownload
import com.rjnsdev.linklift.app.stripPlaylistQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases for supported sources (YouTube & Instagram only).
 */
class SupportedSourcesUrlTest {

    // =========================================================================
    // 1. YouTube Tests
    // =========================================================================

    @Test
    fun testYouTubeHostRecognition() {
        val validYouTubeUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/abcdef12345"
        )
        for (url in validYouTubeUrls) {
            assertTrue("Expected isYtDlpSupportedHost=true for $url", isYtDlpSupportedHost(url))
            assertTrue("Expected isYouTubeUrl=true for $url", isYouTubeUrl(url))
            assertTrue("Expected isSupportedMediaUrl=true for $url", isSupportedMediaUrl(url))
        }
    }

    @Test
    fun testYouTubePlaylistAndChannelDetection() {
        val playlistUrls = listOf(
            "https://www.youtube.com/playlist?list=PL1234567890ABCDEF",
            "https://music.youtube.com/playlist?list=PL1234567890ABCDEF",
            "https://www.youtube.com/channel/UC1234567890",
            "https://www.youtube.com/c/CreatorName",
            "https://www.youtube.com/user/Username",
            "https://www.youtube.com/@ChannelHandle",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890ABCDEF",
            "https://youtu.be/dQw4w9WgXcQ?list=PL1234567890ABCDEF"
        )
        for (url in playlistUrls) {
            assertTrue("Expected looksLikePlaylistUrl=true for $url", looksLikePlaylistUrl(url))
        }

        val singleVideoUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/abcdef12345"
        )
        for (url in singleVideoUrls) {
            assertFalse("Expected looksLikePlaylistUrl=false for $url", looksLikePlaylistUrl(url))
        }
    }

    @Test
    fun testExplicitPlaylistUrlDetection() {
        assertTrue(isExplicitPlaylistUrl("https://www.youtube.com/playlist?list=PL1234567890"))
        assertTrue(isExplicitPlaylistUrl("https://youtube.com/playlist?list=PL1234567890"))
        assertTrue(isExplicitPlaylistUrl("https://music.youtube.com/playlist?list=PL1234567890"))
        assertFalse(isExplicitPlaylistUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890"))
        assertFalse(isExplicitPlaylistUrl("https://youtu.be/dQw4w9WgXcQ?list=PL1234567890"))
        assertFalse(isExplicitPlaylistUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun testStripPlaylistQuery() {
        val withList = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890&index=2"
        val stripped = stripPlaylistQuery(withList)
        assertTrue(stripped.contains("v=dQw4w9WgXcQ"))
        assertFalse(stripped.contains("list="))
        assertFalse(stripped.contains("index="))

        val shortWithList = "https://youtu.be/dQw4w9WgXcQ?list=PL1234567890"
        val shortStripped = stripPlaylistQuery(shortWithList)
        assertTrue(shortStripped.contains("dQw4w9WgXcQ"))
        assertFalse(shortStripped.contains("list="))
    }

    @Test
    fun testNormalizeBatchEntryUrl() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            normalizeBatchEntryUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            normalizeBatchEntryUrl("/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            normalizeBatchEntryUrl("watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            normalizeBatchEntryUrl("dQw4w9WgXcQ")
        )
    }

    // =========================================================================
    // 2. Instagram Tests
    // =========================================================================

    @Test
    fun testInstagramHostAndRefreshRules() {
        val instagramUrls = listOf(
            "https://www.instagram.com/p/C123456789/",
            "https://instagram.com/reel/D987654321/",
            "https://m.instagram.com/reels/E1122334455/",
            "https://instagr.am/p/F99887766/"
        )
        for (url in instagramUrls) {
            assertTrue("Expected isYtDlpSupportedHost=true for $url", isYtDlpSupportedHost(url))
            assertTrue("Expected isInstagramUrl=true for $url", isInstagramUrl(url))
            assertTrue("Expected isSupportedMediaUrl=true for $url", isSupportedMediaUrl(url))
            assertTrue("Instagram link must trigger URL refresh before download", shouldRefreshUrlBeforeDownload(url))
            assertFalse("Instagram post is single item, not playlist", looksLikePlaylistUrl(url))
        }
    }

    // =========================================================================
    // 3. Unsupported Platforms (TikTok, Twitter/X, Reddit, Pinterest, SoundCloud, etc.)
    // =========================================================================

    @Test
    fun testUnsupportedPlatformsRejected() {
        val unsupportedUrls = listOf(
            "https://www.tiktok.com/@creator/video/1234567890123456789",
            "https://vt.tiktok.com/ZS1234567/",
            "https://x.com/user/status/1234567890",
            "https://twitter.com/user/status/1234567890",
            "https://www.facebook.com/watch/?v=123456789",
            "https://fb.watch/ab12cd34ef/",
            "https://www.reddit.com/r/videos/comments/abc123/sample_video/",
            "https://redd.it/abc123",
            "https://www.pinterest.com/pin/123456789012345678/",
            "https://pin.it/7a8b9c0d",
            "https://soundcloud.com/artist/track-title",
            "https://imgur.com/gallery/a1b2c3d",
            "https://vimeo.com/123456789",
            "https://clips.twitch.tv/SampleClipId",
            "https://streamable.com/xyz123",
            "https://www.dailymotion.com/video/x8sample",
            "https://rumble.com/v12345-sample-video.html",
            "https://example.com/index.html"
        )

        for (url in unsupportedUrls) {
            assertFalse("Expected isSupportedMediaUrl=false for $url", isSupportedMediaUrl(url))
            assertFalse("Expected isYouTubeUrl=false for $url", isYouTubeUrl(url))
            assertFalse("Expected isInstagramUrl=false for $url", isInstagramUrl(url))
            assertFalse("Expected isYtDlpSupportedHost=false for $url", isYtDlpSupportedHost(url))
            assertFalse("Expected looksLikePlaylistUrl=false for $url", looksLikePlaylistUrl(url))
        }
    }

    // =========================================================================
    // 4. URL Extraction Tests (extractAllUrls, extractFirstUrl)
    // =========================================================================

    @Test
    fun testUrlExtractionFromText() {
        val sampleText = """
            Check out this cool video https://www.youtube.com/watch?v=dQw4w9WgXcQ!
            Also look at this post: https://www.instagram.com/p/C123456789/, and http://example.com.
        """.trimIndent()

        val extracted = extractAllUrls(sampleText)
        assertEquals(3, extracted.size)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", extracted[0])
        assertEquals("https://www.instagram.com/p/C123456789/", extracted[1])
        assertEquals("http://example.com", extracted[2])

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", extractFirstUrl(sampleText))
        assertNull(extractFirstUrl("No links in this text message"))
    }

    // =========================================================================
    // 5. MediaKind Determination Tests
    // =========================================================================

    @Test
    fun testMediaKindFromMimeAndExtension() {
        // Video mime types & extensions
        assertEquals(MediaKind.Video, MediaKind.fromMimeType("video/mp4", "https://cdn.com/file"))
        assertEquals(MediaKind.Video, MediaKind.fromMimeType("application/octet-stream", "https://cdn.com/file.mp4"))
        assertEquals(MediaKind.Video, MediaKind.fromMimeType("", "https://cdn.com/file.mkv"))
        assertEquals(MediaKind.Video, MediaKind.fromMimeType(null, "https://cdn.com/file.webm"))
        assertEquals(MediaKind.Video, MediaKind.fromMimeType(null, "https://cdn.com/playlist.m3u8"))

        // Audio mime types & extensions
        assertEquals(MediaKind.Audio, MediaKind.fromMimeType("audio/mpeg", "https://cdn.com/track"))
        assertEquals(MediaKind.Audio, MediaKind.fromMimeType("audio/aac", "https://cdn.com/track"))
        assertEquals(MediaKind.Audio, MediaKind.fromMimeType("", "https://cdn.com/song.mp3"))
        assertEquals(MediaKind.Audio, MediaKind.fromMimeType(null, "https://cdn.com/song.wav"))
        assertEquals(MediaKind.Audio, MediaKind.fromMimeType(null, "https://cdn.com/song.m4a"))

        // Image mime types & extensions
        assertEquals(MediaKind.Image, MediaKind.fromMimeType("image/png", "https://cdn.com/pic"))
        assertEquals(MediaKind.Image, MediaKind.fromMimeType("image/jpeg", "https://cdn.com/photo"))
        assertEquals(MediaKind.Image, MediaKind.fromMimeType("", "https://cdn.com/photo.webp"))
        assertEquals(MediaKind.Image, MediaKind.fromMimeType(null, "https://cdn.com/animation.gif"))

        // Unknown
        assertEquals(MediaKind.Unknown, MediaKind.fromMimeType("text/html", "https://example.com/page"))
    }

    @Test
    fun testBuildGoogleVideoChunkUrl() {
        val baseWithExistingRange = "https://rr1---sn.googlevideo.com/videoplayback?expire=123&range=0-1024&rn=1&clen=5000"
        val chunkUrl1 = buildGoogleVideoChunkUrl(baseWithExistingRange, 10485760L, 20971519L, 2)
        assertTrue("Must contain new range parameter", chunkUrl1.contains("range=10485760-20971519"))
        assertTrue("Must contain new rn parameter", chunkUrl1.contains("rn=2"))
        assertFalse("Must not contain old range", chunkUrl1.contains("range=0-1024"))

        val baseWithoutRange = "https://rr1---sn.googlevideo.com/videoplayback?expire=123&clen=5000"
        val chunkUrl2 = buildGoogleVideoChunkUrl(baseWithoutRange, 0L, 10485759L, 1)
        assertTrue(chunkUrl2.contains("range=0-10485759"))
        assertTrue(chunkUrl2.contains("rn=1"))
    }

    @Test
    fun testIsYouTubeAuthRequired() {
        // Non-YouTube URLs never require YouTube auth
        assertFalse(isYouTubeAuthRequired(isYt = false, hasCookies = false, errMsg = "403 Forbidden"))

        // Unauthenticated YouTube errors require auth
        assertTrue(isYouTubeAuthRequired(isYt = true, hasCookies = false, errMsg = "Sign in to confirm you're not a bot"))
        assertTrue(isYouTubeAuthRequired(isYt = true, hasCookies = false, errMsg = "HTTP 403"))
        assertTrue(isYouTubeAuthRequired(isYt = true, hasCookies = false, errMsg = "This playlist is private"))

        // When user ALREADY has cookies:
        // Generic 403 or chunk errors must NOT prompt user to sign in
        assertFalse(isYouTubeAuthRequired(isYt = true, hasCookies = true, errMsg = "Download chunk failed: HTTP 403 for url"))
        assertFalse(isYouTubeAuthRequired(isYt = true, hasCookies = true, errMsg = "HTTP 403 (GoogleVideo CDN rejected range 0-1000)"))
        assertFalse(isYouTubeAuthRequired(isYt = true, hasCookies = true, errMsg = "Connection reset by peer"))

        // When cookies actually expired / session invalid:
        assertTrue(isYouTubeAuthRequired(isYt = true, hasCookies = true, errMsg = "Sign in to confirm your identity"))
        assertTrue(isYouTubeAuthRequired(isYt = true, hasCookies = true, errMsg = "Your cookie session has expired"))
    }
}
