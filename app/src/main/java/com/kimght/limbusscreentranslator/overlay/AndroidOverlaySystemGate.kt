package com.kimght.limbusscreentranslator.overlay

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidOverlaySystemGate @Inject constructor(
    @ApplicationContext private val context: Context,
) : OverlaySystemGate {
    override fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)
    override fun startOverlayService() = OverlayService.start(context)
    override fun stopOverlayService() = OverlayService.stop(context)
}
