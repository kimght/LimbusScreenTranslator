package com.kimght.limbusscreentranslator.overlay

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class OverlayLauncherTest {

    private class FakeGate(var drawPermission: Boolean = true) : OverlaySystemGate {
        var startCalls = 0
        var stopCalls = 0
        override fun canDrawOverlays(): Boolean = drawPermission
        override fun startOverlayService() { startCalls++ }
        override fun stopOverlayService() { stopCalls++ }
    }

    private lateinit var settingsDir: File
    private lateinit var settings: SettingsRepository
    private lateinit var runningState: OverlayRunningState
    private lateinit var gate: FakeGate
    private lateinit var launcher: OverlayLauncher

    @Before
    fun setUp() {
        settingsDir = Files.createTempDirectory("settings").toFile()
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(
                produceFile = { File(settingsDir, "settings.preferences_pb") },
            ),
        )
        runningState = OverlayRunningState()
        gate = FakeGate()
        launcher = OverlayLauncher(settings, runningState, gate)
    }

    @After
    fun tearDown() {
        settingsDir.deleteRecursively()
    }

    @Test
    fun `cannot start without an active pack`() = runTest {
        assertFalse(launcher.canStart())
        assertFalse(launcher.start())
        assertEquals(0, gate.startCalls)
    }

    @Test
    fun `cannot start without the draw-overlay permission`() = runTest {
        settings.setActiveLocalizationId("Github:ru-mtl")
        gate.drawPermission = false

        assertFalse(launcher.canStart())
        assertFalse(launcher.start())
        assertEquals(0, gate.startCalls)
    }

    @Test
    fun `starts when a pack is active and the permission is granted`() = runTest {
        settings.setActiveLocalizationId("Github:ru-mtl")

        assertTrue(launcher.canStart())
        assertTrue(launcher.start())
        assertEquals(1, gate.startCalls)
    }

    @Test
    fun `toggle stops a running overlay`() = runTest {
        settings.setActiveLocalizationId("Github:ru-mtl")
        runningState.set(true)

        assertEquals(OverlayLauncher.ToggleResult.STOPPED, launcher.toggle())
        assertEquals(1, gate.stopCalls)
        assertEquals(0, gate.startCalls)
    }

    @Test
    fun `toggle starts a stopped overlay when it can`() = runTest {
        settings.setActiveLocalizationId("Github:ru-mtl")

        assertEquals(OverlayLauncher.ToggleResult.STARTED, launcher.toggle())
        assertEquals(1, gate.startCalls)
    }

    @Test
    fun `toggle reports missing setup instead of starting`() = runTest {
        assertEquals(OverlayLauncher.ToggleResult.NEEDS_SETUP, launcher.toggle())
        assertEquals(0, gate.startCalls)
    }
}
