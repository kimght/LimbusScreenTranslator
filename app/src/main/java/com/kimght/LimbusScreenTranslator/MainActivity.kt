package com.kimght.LimbusScreenTranslator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kimght.LimbusScreenTranslator.core.i18n.ProvideUiLanguage
import com.kimght.LimbusScreenTranslator.core.i18n.localizedTo
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.datastore.Settings as AppSettings
import com.kimght.LimbusScreenTranslator.feature.navigation.ManagerApp
import com.kimght.LimbusScreenTranslator.overlay.OverlayService
import com.kimght.LimbusScreenTranslator.ui.theme.LimbusScreenTranslatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    @Volatile
    private var currentLanguage: String = AppSettings.defaultUiLanguage()

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
        } else {
            Toast.makeText(
                this,
                localizedTo(currentLanguage).getString(R.string.overlay_permission_required),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startOverlayService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            settings.settings.collect { currentLanguage = it.uiLanguage }
        }
        setContent {
            val languageFlow = remember {
                settings.settings.map { it.uiLanguage }.distinctUntilChanged()
            }
            val language by languageFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings.defaultUiLanguage(),
            )
            LimbusScreenTranslatorTheme {
                ProvideUiLanguage(language) {
                    ManagerApp(
                        onOpenOverlay = ::launchOverlay,
                        onCloseOverlay = { OverlayService.stop(this) },
                    )
                }
            }
        }
    }

    private fun launchOverlay() {
        if (needsNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startOverlayService()
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun startOverlayService() {
        if (Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
        } else {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()),
            )
        }
    }
}
