package com.rjnsdev.linklift.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTheme

class ShareActivity : ComponentActivity() {
    private val viewModel: LinkLiftViewModel by viewModels()

    private val requestPostNotificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

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
        setContent {
            LinkLiftTheme(darkTheme = true) {
                ShareOverlaySheet(
                    viewModel = viewModel,
                    onDismiss = { dismissAndExit() },
                    ensureNotificationPermission = ::requestPostNotificationsIfNeeded,
                )
            }
        }
        handleIncomingShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShare(intent)
    }

    private fun dismissAndExit() {
        finishAndRemoveTask()
    }

    private fun handleIncomingShare(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) {
            dismissAndExit()
            return
        }
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.getItemAt(0)?.text?.toString()
            ?: intent.dataString

        val handled = viewModel.handleSharedText(sharedText, isFromShare = true)
        if (!handled) {
            dismissAndExit()
        }
    }
}
