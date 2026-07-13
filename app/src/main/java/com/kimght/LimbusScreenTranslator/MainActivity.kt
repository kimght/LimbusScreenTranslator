package com.kimght.LimbusScreenTranslator

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.kimght.LimbusScreenTranslator.feature.navigation.ManagerApp
import com.kimght.LimbusScreenTranslator.overlay.OverlayService
import com.kimght.LimbusScreenTranslator.ui.theme.LimbusScreenTranslatorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
        } else {
            Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LimbusScreenTranslatorTheme {
                ManagerApp(
                    onOpenOverlay = ::launchOverlay,
                    onCloseOverlay = { OverlayService.stop(this) },
                )
            }
        }
    }

    private fun launchOverlay() {
        if (Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
        } else {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
            )
        }
    }
}
