package com.rjnsdev.linklift.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DownloadManager.ACTION_NOTIFICATION_CLICKED) return

        val downloadIds = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
        val downloadId = downloadIds?.firstOrNull()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_DOWNLOADS
            putExtra(EXTRA_TARGET_SCREEN, AppScreen.Downloads.name)
            if (downloadId != null) {
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        runCatching {
            context.startActivity(launchIntent)
        }
    }
}
