package com.rjnsdev.linklift.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel: LinkLiftViewModel by viewModels()

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!viewModel.handleBackPress()) {
                finish()
            }
        }
    }

    /**
     * Android 13+ requires [Manifest.permission.POST_NOTIFICATIONS] at runtime
     * or the system hides **all** notifications from this app — including
     * DownloadManager's in-progress and finished download notifications.
     */
    private val requestPostNotificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    /** Android 13+ (API 33): runtime permission or no DownloadManager / app notifications. */
    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPostNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        setContent {
            LinkLiftRoot(
                viewModel = viewModel,
                ensureNotificationPermission = ::requestPostNotificationsIfNeeded,
            )
        }
        handleIncomingShareIntent(intent)
        handleIncomingNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
        handleIncomingNotificationIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.getItemAt(0)?.text?.toString()
            ?: intent.dataString

        val handled = viewModel.handleSharedText(sharedText, isFromShare = true)
        if (handled) {
            setIntent(Intent())
        }
    }

    private fun handleIncomingNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val targetScreen = intent.getStringExtra(EXTRA_TARGET_SCREEN)
        if (action == ACTION_VIEW_DOWNLOADS || targetScreen == AppScreen.Downloads.name) {
            val downloadId = if (intent.hasExtra(EXTRA_DOWNLOAD_ID)) {
                intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L).takeIf { it != -1L }
            } else {
                null
            }
            viewModel.handleNotificationNavigation(downloadId)
            setIntent(Intent())
        }
    }
}

