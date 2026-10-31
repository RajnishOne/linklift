package com.rjnsdev.linklift.app

import com.rjnsdev.linklift.app.ui.theme.LinkLiftInstagramPink
import com.rjnsdev.linklift.app.ui.theme.LinkLiftYouTubeRed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformBrandTest {

    @Test
    fun testYouTubePlatformRecognition() {
        val urls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://m.youtube.com/shorts/12345",
        )
        for (url in urls) {
            val brand = getPlatformBrand(url)
            assertNotNull("Expected brand for $url", brand)
            assertEquals("YouTube", brand!!.name)
            assertEquals("YouTube", brand.first)
            assertEquals(LinkLiftYouTubeRed, brand.color)
            assertEquals(LinkLiftYouTubeRed, brand.second)
            assertNotNull(brand.icon)
            assertNotNull(brand.third)

            // Destructuring test
            val (name, color, icon) = brand
            assertEquals("YouTube", name)
            assertEquals(LinkLiftYouTubeRed, color)
            assertNotNull(icon)
        }
    }

    @Test
    fun testInstagramPlatformRecognition() {
        val urls = listOf(
            "https://www.instagram.com/p/C69abcxyz/",
            "https://www.instagram.com/reel/C-123456789/",
            "https://instagr.am/reel/abc123456/",
        )
        for (url in urls) {
            val brand = getPlatformBrand(url)
            assertNotNull("Expected brand for $url", brand)
            assertEquals("Instagram", brand!!.name)
            assertEquals("Instagram", brand.first)
            assertEquals(LinkLiftInstagramPink, brand.color)
            assertEquals(LinkLiftInstagramPink, brand.second)
            assertNotNull(brand.icon)
            assertNotNull(brand.third)
        }
    }

    @Test
    fun testHostFallbackRecognition() {
        val ytHost = getPlatformBrand(url = null, host = "youtube.com")
        assertNotNull(ytHost)
        assertEquals("YouTube", ytHost!!.name)

        val igHost = getPlatformBrand(url = "", host = "instagram.com")
        assertNotNull(igHost)
        assertEquals("Instagram", igHost!!.name)
    }

    @Test
    fun testUnknownSourceReturnsNull() {
        assertNull(getPlatformBrand("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
        assertNull(getPlatformBrand(null, "example.com"))
        assertNull(getPlatformBrand(null, null))
    }
}
