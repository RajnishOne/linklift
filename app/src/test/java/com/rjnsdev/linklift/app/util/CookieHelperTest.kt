package com.rjnsdev.linklift.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CookieHelperTest {

    @Test
    fun testCookieEntryToNetscapeLine() {
        val entry = CookieHelper.CookieEntry(
            domain = "youtube.com",
            path = "/",
            isSecure = true,
            expiry = 1750000000L,
            name = "LOGIN_INFO",
            value = "sample_value_123",
        )
        val line = entry.toNetscapeLine()
        assertEquals(".youtube.com\tTRUE\t/\tTRUE\t1750000000\tLOGIN_INFO\tsample_value_123", line)
    }

    @Test
    fun testFormatRawCookieStringToNetscape() {
        val raw = "LOGIN_INFO=abc123xyz; __Secure-3PSID=sec_token_999; SID=sid_val"
        val netscape = CookieHelper.formatRawCookieStringToNetscape(raw, defaultDomain = ".youtube.com")

        assertTrue(netscape.contains("# Netscape HTTP Cookie File"))
        assertTrue(netscape.contains(".youtube.com\tTRUE\t/\tTRUE\t"))
        assertTrue(netscape.contains("LOGIN_INFO\tabc123xyz"))
        assertTrue(netscape.contains("__Secure-3PSID\tsec_token_999"))
        assertTrue(netscape.contains("SID\tsid_val"))
    }

    @Test
    fun testFormatCookieEntriesToNetscape_deduplicatesAndSkipsBlankValues() {
        val entries = listOf(
            CookieHelper.CookieEntry(
                domain = ".youtube.com",
                name = "SID",
                value = "valid_sid",
            ),
            CookieHelper.CookieEntry(
                domain = ".youtube.com",
                name = "SID",
                value = "", // Blank value that should not override valid_sid
            ),
            CookieHelper.CookieEntry(
                domain = ".youtube.com",
                name = "BLANK_COOKIE",
                value = "   ", // All whitespace
            ),
            CookieHelper.CookieEntry(
                domain = "youtube.com",
                name = "HSID",
                value = "valid_hsid",
            )
        )
        val result = CookieHelper.formatCookieEntriesToNetscape(entries)
        assertTrue(result.contains("valid_sid"))
        assertTrue(result.contains("valid_hsid"))
        assertFalse(result.contains("BLANK_COOKIE"))
    }

    @Test
    fun testFormatRawCookieStringToNetscape_skipsEmptyValues() {
        val raw = "SID=valid_sid; EMPTY=; HSID=valid_hsid; "
        val result = CookieHelper.formatRawCookieStringToNetscape(raw)
        assertTrue(result.contains("SID\tvalid_sid"))
        assertTrue(result.contains("HSID\tvalid_hsid"))
        assertFalse(result.contains("EMPTY"))
    }
}
