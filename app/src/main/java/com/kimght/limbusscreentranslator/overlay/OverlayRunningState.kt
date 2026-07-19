package com.kimght.limbusscreentranslator.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayRunningState @Inject constructor() {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun set(running: Boolean) {
        _isRunning.value = running
    }
}
