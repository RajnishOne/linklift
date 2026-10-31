package com.rjnsdev.linklift.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationNavigationTest {

    @Test
    fun testNavigationConstants() {
        assertEquals("com.rjnsdev.linklift.action.VIEW_DOWNLOADS", ACTION_VIEW_DOWNLOADS)
        assertEquals("extra_target_screen", EXTRA_TARGET_SCREEN)
        assertEquals("extra_download_id", EXTRA_DOWNLOAD_ID)
    }

    @Test
    fun testTargetScreenValueMatchesAppScreen() {
        assertEquals("Downloads", AppScreen.Downloads.name)
    }
}
