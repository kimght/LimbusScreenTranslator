package com.kimght.limbusscreentranslator.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SettingsRepositoryTest {

    private lateinit var tmp: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        tmp = Files.createTempDirectory("settings").toFile()
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tmp, "settings.preferences_pb") },
        )
        repo = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        tmp.deleteRecursively()
    }

    @Test
    fun `defaults are returned when nothing is stored`() = runTest {
        val s = repo.settings.first()
        assertEquals(Settings.DEFAULT_OPACITY, s.opacity)
        assertEquals(Settings.defaultUiLanguage(), s.uiLanguage)
        assertNull(s.activeLocalizationId)
    }

    @Test
    fun `a corrupt preferences file yields defaults instead of crashing`() = runTest {
        val corruptDir = Files.createTempDirectory("corrupt").toFile()
        val file = File(corruptDir, "settings.preferences_pb")
        file.writeBytes(byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8))
        val corruptRepo = SettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = { file }),
        )

        val s = corruptRepo.settings.first()

        assertEquals(Settings.DEFAULT_OPACITY, s.opacity)
        assertNull(s.activeLocalizationId)
        corruptDir.deleteRecursively()
    }

    @Test
    fun `opacity is clamped to its allowed range`() = runTest {
        repo.setOpacity(5f)
        assertEquals(Settings.MAX_OPACITY, repo.settings.first().opacity)
        repo.setOpacity(0f)
        assertEquals(Settings.MIN_OPACITY, repo.settings.first().opacity)
    }

    @Test
    fun `active localization can be set and cleared`() = runTest {
        repo.setActiveLocalizationId("ru-Test")
        assertEquals("ru-Test", repo.settings.first().activeLocalizationId)
        repo.setActiveLocalizationId(null)
        assertNull(repo.settings.first().activeLocalizationId)
    }

    @Test
    fun `panel position round-trips`() = runTest {
        repo.setPanelPosition(120, 340)
        val s = repo.settings.first()
        assertEquals(120, s.panelX)
        assertEquals(340, s.panelY)
    }

    @Test
    fun `positions default to null until saved`() = runTest {
        val s = repo.settings.first()
        assertNull(s.panelX)
        assertNull(s.panelY)
        assertNull(s.minimizedX)
        assertNull(s.minimizedY)
    }

    @Test
    fun `a saved top-left position is distinct from never-saved`() = runTest {
        repo.setPanelPosition(0, 0)
        repo.setMinimizedPosition(0, 0)
        val s = repo.settings.first()
        assertEquals(0, s.panelX)
        assertEquals(0, s.panelY)
        assertEquals(0, s.minimizedX)
        assertEquals(0, s.minimizedY)
    }

    @Test
    fun `overlay size is clamped to its allowed range`() = runTest {
        repo.setOverlaySize(9999, 9999)
        repo.settings.first().let {
            assertEquals(Settings.MAX_OVERLAY_WIDTH, it.overlayWidth)
            assertEquals(Settings.MAX_OVERLAY_CONTENT_HEIGHT, it.overlayContentHeight)
        }
        repo.setOverlaySize(0, 0)
        repo.settings.first().let {
            assertEquals(Settings.MIN_OVERLAY_WIDTH, it.overlayWidth)
            assertEquals(Settings.MIN_OVERLAY_CONTENT_HEIGHT, it.overlayContentHeight)
        }
    }

    @Test
    fun `overlay size defaults when nothing stored`() = runTest {
        repo.settings.first().let {
            assertEquals(Settings.DEFAULT_OVERLAY_WIDTH, it.overlayWidth)
            assertEquals(Settings.DEFAULT_OVERLAY_CONTENT_HEIGHT, it.overlayContentHeight)
        }
    }

    @Test
    fun `minimized position round-trips`() = runTest {
        repo.setMinimizedPosition(40, 700)
        val s = repo.settings.first()
        assertEquals(40, s.minimizedX)
        assertEquals(700, s.minimizedY)
    }

    @Test
    fun `resetToDefaults clears every stored preference`() = runTest {
        repo.setOpacity(0.5f)
        repo.setTextSize(1.4f)
        repo.setUiLanguage("ru")
        repo.setPanelPosition(10, 20)
        repo.setMinimizedPosition(30, 40)
        repo.setOverlaySize(400, 200)
        repo.setActiveLocalizationId("ru-Test")
        repo.setSelectedBrowsingSource("Github")

        repo.resetToDefaults()

        val s = repo.settings.first()
        assertEquals(Settings.DEFAULT_OPACITY, s.opacity)
        assertEquals(Settings.DEFAULT_TEXT_SIZE, s.textSize)
        assertEquals(Settings.defaultUiLanguage(), s.uiLanguage)
        assertNull(s.panelX)
        assertNull(s.panelY)
        assertNull(s.minimizedX)
        assertNull(s.minimizedY)
        assertEquals(Settings.DEFAULT_OVERLAY_WIDTH, s.overlayWidth)
        assertEquals(Settings.DEFAULT_OVERLAY_CONTENT_HEIGHT, s.overlayContentHeight)
        assertNull(s.activeLocalizationId)
        assertNull(s.selectedBrowsingSource)
    }
}
