package com.kimght.limbusscreentranslator.overlay

import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface OverlaySystemGate {
    fun canDrawOverlays(): Boolean
    fun startOverlayService()
    fun stopOverlayService()
}

@Singleton
class OverlayLauncher @Inject constructor(
    private val settings: SettingsRepository,
    private val runningState: OverlayRunningState,
    private val gate: OverlaySystemGate,
) {
    enum class ToggleResult { STARTED, STOPPED, NEEDS_SETUP }

    val isRunning: StateFlow<Boolean> = runningState.isRunning

    suspend fun canStart(): Boolean =
        settings.settings.first().activeLocalizationId != null && gate.canDrawOverlays()

    suspend fun start(): Boolean {
        if (!canStart()) return false
        gate.startOverlayService()
        return true
    }

    fun stop() = gate.stopOverlayService()

    suspend fun toggle(): ToggleResult = when {
        runningState.isRunning.value -> {
            stop()
            ToggleResult.STOPPED
        }

        start() -> ToggleResult.STARTED
        else -> ToggleResult.NEEDS_SETUP
    }
}
