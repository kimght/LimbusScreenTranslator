package com.kimght.limbusscreentranslator

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kimght.limbusscreentranslator.overlay.OverlayLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class QuickLaunchActivity : ComponentActivity() {

    @Inject
    lateinit var overlayLauncher: OverlayLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val gameIntent = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
            val overlayReady = gameIntent != null &&
                (overlayLauncher.isRunning.value || overlayLauncher.start())
            if (overlayReady) {
                startActivity(gameIntent)
            } else {
                startActivity(Intent(this@QuickLaunchActivity, MainActivity::class.java))
            }
            finish()
        }
    }

    companion object {
        const val GAME_PACKAGE = "com.ProjectMoon.LimbusCompany"
    }
}
