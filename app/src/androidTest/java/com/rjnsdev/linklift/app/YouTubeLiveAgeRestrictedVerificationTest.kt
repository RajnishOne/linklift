package com.rjnsdev.linklift.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.rjnsdev.linklift.app.util.CookieHelper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YouTubeLiveAgeRestrictedVerificationTest {

    private val dummyAuthCookies = """
        # Netscape HTTP Cookie File
        .youtube.com	TRUE	/	TRUE	1830254001	LOGIN_INFO	dummy_login_info_token
        .youtube.com	TRUE	/	TRUE	1830254001	__Secure-3PSID	dummy_secure_3psid
        .youtube.com	TRUE	/	TRUE	1830254001	__Secure-3PAPISID	dummy_apisid
        .youtube.com	TRUE	/	TRUE	1811266097	VISITOR_INFO1_LIVE	kO-SjU88UiU
        .youtube.com	TRUE	/	TRUE	0	YSC	AkEfp0gcpe4
    """.trimIndent()

    @Test
    fun testYouTubeLiveUrlParsing() {
        val liveUrl = "https://www.youtube.com/live/R8__7E4nzIU?si=Pahg5fHiCeH_3ipD"
        val id = extractYouTubeVideoId(liveUrl)
        assertEquals("R8__7E4nzIU", id)

        val thumb = extractYouTubeThumbnailUrl(liveUrl)
        assertEquals("https://i.ytimg.com/vi/R8__7E4nzIU/hqdefault.jpg", thumb)

        val watchUrl = "https://www.youtube.com/watch?v=R8__7E4nzIU"
        assertEquals("R8__7E4nzIU", extractYouTubeVideoId(watchUrl))
    }

    @Test
    fun testCookieValidation() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            CookieHelper.clearCookies(appContext)
            assertFalse(CookieHelper.hasValidCookies(appContext))

            // Guest-only cookies (false positive before our fix)
            val guestCookies = """
                # Netscape HTTP Cookie File
                .youtube.com	TRUE	/	TRUE	1811266097	VISITOR_INFO1_LIVE	kO-SjU88UiU
                .youtube.com	TRUE	/	TRUE	0	YSC	AkEfp0gcpe4
            """.trimIndent()
            CookieHelper.importFromText(appContext, guestCookies).getOrThrow()
            assertFalse(CookieHelper.hasValidCookies(appContext))

            // Auth cookies with session keys
            CookieHelper.importFromText(appContext, dummyAuthCookies).getOrThrow()
            assertTrue(CookieHelper.hasValidCookies(appContext))
        } finally {
            CookieHelper.clearCookies(appContext)
        }
    }

    @Test
    fun testStandardVideoResolution() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(appContext))
        }
        val python = Python.getInstance()
        val resolver = python.getModule("generic_media_resolver")
        val t0 = System.currentTimeMillis()
        val payloadStr = resolver.callAttr("resolve_url", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", null).toString()
        val t1 = System.currentTimeMillis()
        android.util.Log.d("LinkLiftTest", "Standard video resolved in ${t1 - t0} ms")

        val json = JSONObject(payloadStr)
        val formats = json.optJSONArray("formats")
        assertNotNull(formats)
        assertTrue(formats!!.length() > 0)
    }

    @Test
    fun testAgeRestrictedVideoResolutionWhenCookiesPresent() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        if (!CookieHelper.hasValidCookies(appContext)) {
            // Skip age-restricted resolution if no real authenticated cookies are imported on this test environment
            return
        }
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(appContext))
        }
        val python = Python.getInstance()
        val cookiePath = CookieHelper.getCookieFilePath(appContext)
        assertNotNull(cookiePath)

        val resolver = python.getModule("generic_media_resolver")
        val targetUrl = "https://www.youtube.com/watch?v=R8__7E4nzIU"
        val payloadStr = resolver.callAttr("resolve_url", targetUrl, cookiePath).toString()

        val json = JSONObject(payloadStr)
        val title = json.optString("title", "")
        assertTrue("Title should contain GTA VI or Leonida, was: $title", title.contains("GTA VI") || title.contains("Leonida"))

        val formats = json.optJSONArray("formats")
        assertNotNull("Formats array should not be null", formats)
        assertTrue("Formats array should not be empty", formats!!.length() > 0)

        var foundMediaUrl = false
        for (i in 0 until formats.length()) {
            val fmt = formats.getJSONObject(i)
            val mediaUrl = fmt.optString("media_url", "")
            if (mediaUrl.isNotBlank() && (mediaUrl.contains("googlevideo.com") || mediaUrl.startsWith("http"))) {
                foundMediaUrl = true
                break
            }
        }
        assertTrue("At least one playable media URL should be present", foundMediaUrl)
    }
}
