package com.kimght.limbusscreentranslator.overlay

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEntity
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEpisodeEntity
import com.kimght.limbusscreentranslator.data.install.InstallManager
import com.kimght.limbusscreentranslator.data.install.PackInstaller
import com.kimght.limbusscreentranslator.data.install.RoomPackContentWriter
import com.kimght.limbusscreentranslator.data.install.ScenarioContent
import com.kimght.limbusscreentranslator.data.network.Downloader
import com.kimght.limbusscreentranslator.data.network.LocalizationApi
import com.kimght.limbusscreentranslator.data.repository.CatalogCache
import com.kimght.limbusscreentranslator.data.repository.LocalizationRepository
import com.kimght.limbusscreentranslator.data.repository.OverlayStateRepository
import com.kimght.limbusscreentranslator.data.repository.ScenarioRepository
import com.kimght.limbusscreentranslator.data.repository.SourceRepository
import com.kimght.limbusscreentranslator.domain.model.DialogueLine
import com.kimght.limbusscreentranslator.domain.model.InstalledPack
import com.kimght.limbusscreentranslator.domain.model.PackKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OverlayControllerTest {

    private lateinit var db: LimbusDatabase
    private lateinit var settingsDir: File
    private lateinit var cacheRoot: File
    private lateinit var settings: SettingsRepository
    private lateinit var localizations: LocalizationRepository
    private lateinit var scenarios: ScenarioRepository
    private lateinit var overlayState: OverlayStateRepository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private class NoopDownloader : Downloader {
        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long = 0L
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cacheRoot = Files.createTempDirectory("cache").toFile()
        settingsDir = Files.createTempDirectory("settings").toFile()

        val api = LocalizationApi(OkHttpClient())
        val writer = RoomPackContentWriter(db)
        val installer = PackInstaller(
            downloader = NoopDownloader(),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val installManager = InstallManager(installer, appScope)
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(settingsDir, "settings.preferences_pb") },
        )
        settings = SettingsRepository(dataStore)
        scenarios = ScenarioRepository(db, db.scenarioDao(), db.chapterDao(), api)
        localizations = LocalizationRepository(
            api = api,
            installedPackDao = db.installedPackDao(),
            installManager = installManager,
            settings = settings,
            contentWriter = writer,
            scenarios = scenarios,
            sources = SourceRepository(db.sourceDao(), settings),
            catalogCache = CatalogCache(),
        )
        overlayState = OverlayStateRepository(db.overlayStateDao(), clock = { 42L })
    }

    @After
    fun tearDown() {
        db.close()
        cacheRoot.deleteRecursively()
        settingsDir.deleteRecursively()
        appScope.cancel()
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<OverlayUiState>.awaitFirst(
        limit: Int = 10,
        predicate: (OverlayUiState) -> Boolean,
    ): OverlayUiState? {
        repeat(limit) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
        return null
    }

    @Test
    fun `setOrientation portrait forces minimized`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.setOrientation(true)
        controller.uiState.test {
            val found = awaitFirst { it.minimized }
            assertTrue("Expected minimized=true", found != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setOrientation portrait clears resizing`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            val entered = awaitFirst { it.resizing }
            assertTrue("Expected resizing=true after selectMode(RESIZE)", entered != null)

            controller.setOrientation(true)
            val cleared = awaitFirst { !it.resizing }
            assertTrue("Expected resizing=false after setOrientation(true)", cleared != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects orientation`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.setOrientation(true)
            val portrait = awaitFirst { it.portrait }
            assertTrue("Expected portrait=true after setOrientation(true)", portrait != null)
            controller.setOrientation(false)
            val landscape = awaitFirst { !it.portrait }
            assertTrue("Expected portrait=false after setOrientation(false)", landscape != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore is no-op while portrait`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.setOrientation(true)
        controller.restore()
        controller.uiState.test {
            val found = awaitFirst { it.minimized }
            assertTrue("Expected minimized=true after restore() while portrait", found != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resize drag-end keeps the draft live for further drags`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            assertTrue("Expected resizing=true", awaitFirst { it.resizing } != null)

            controller.updateResizeDraft(40f, 0f)
            assertTrue("Expected width 400 after first drag", awaitFirst { it.overlayWidth == 400 } != null)

            controller.persistResizeDraft()

            controller.updateResizeDraft(40f, 0f)
            val grown = awaitFirst(limit = 20) { it.overlayWidth == 440 }
            assertTrue("Expected width to keep growing after drag-end, was stuck", grown != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sub-dp resize deltas accumulate instead of being truncated`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            assertTrue("Expected resizing=true", awaitFirst { it.resizing } != null)

            repeat(10) { controller.updateResizeDraft(0.5f, 0.25f) }

            val grown = awaitFirst(limit = 30) {
                it.overlayWidth == 365 && it.overlayContentHeight == 153
            }
            assertTrue("Expected 10×(0.5, 0.25)dp drags to grow 360×150 to 365×153", grown != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `minimize commits an in-progress resize`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            assertTrue("Expected resizing=true", awaitFirst { it.resizing } != null)
            controller.updateResizeDraft(60f, 0f)
            assertTrue("Expected width 420", awaitFirst { it.overlayWidth == 420 } != null)
            controller.minimize()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(420, settings.settings.first { it.overlayWidth == 420 }.overlayWidth)
    }

    @Test
    fun `selecting another section from resize commits the draft`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            assertTrue("Expected resizing=true", awaitFirst { it.resizing } != null)
            controller.updateResizeDraft(60f, 0f)
            assertTrue("Expected width 420", awaitFirst { it.overlayWidth == 420 } != null)

            controller.selectMode(OverlayMode.CHAPTER)
            val switched = awaitFirst { it.mode == OverlayMode.CHAPTER && !it.resizing }
            assertTrue("Expected chapter section after leaving resize", switched != null)

            assertEquals(420, switched?.overlayWidth)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(420, settings.settings.first { it.overlayWidth == 420 }.overlayWidth)
    }

    @Test
    fun `re-selecting the active section keeps the resize session alive`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            assertTrue("Expected resizing=true", awaitFirst { it.resizing } != null)
            controller.updateResizeDraft(40f, 0f)
            assertTrue("Expected width 400", awaitFirst { it.overlayWidth == 400 } != null)

            controller.selectMode(OverlayMode.RESIZE)
            controller.updateResizeDraft(40f, 0f)
            val grown = awaitFirst(limit = 20) { it.overlayWidth == 440 && it.resizing }
            assertTrue("Expected the draft to survive re-selecting RESIZE", grown != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun seedActivePackWithChapters() {
        RoomPackContentWriter(db).replacePack(
            InstalledPack(
                id = "ru-mtl",
                version = "v1",
                sourceName = "Github",
                installedAt = 1L,
                name = "MTL",
                flag = "RU",
                description = "",
            ),
            sequenceOf(
                ScenarioContent(
                    code = "7-1",
                    lines = listOf(
                        DialogueLine(index = 0, speakerName = null, title = null, place = null, text = "hi"),
                    ),
                ),
            ),
        )
        db.chapterDao().insertChapters(
            listOf(ChapterEntity(sourceName = "Github", position = 0, name = "CANTO VII", subtitle = "s")),
        )
        db.chapterDao().insertEpisodes(
            listOf(
                ChapterEpisodeEntity(sourceName = "Github", chapterPosition = 0, position = 0, episodeCode = "7-1"),
            ),
        )
        settings.setActiveLocalizationId(PackKey.of("Github", "ru-mtl"))
    }

    @Test
    fun `resize frames reuse the chapter model instead of rebuilding it`() = runTest {
        seedActivePackWithChapters()
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.uiState.test {
            controller.selectMode(OverlayMode.RESIZE)
            val before = awaitFirst { it.resizing && it.chapters.isNotEmpty() }
            assertTrue("Expected resizing state with chapters", before != null)

            controller.updateResizeDraft(40f, 0f)
            val after = awaitFirst(limit = 20) { it.overlayWidth == 400 }
            assertTrue("Expected width 400 after drag", after != null)

            assertSame(
                "resize-drag frames must not rebuild the chapter rows",
                before!!.chapters,
                after!!.chapters,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore works after landscape`() = runTest {
        val controller = OverlayController(settings, localizations, scenarios, overlayState, backgroundScope)
        controller.setOrientation(true)
        controller.setOrientation(false)
        controller.restore()
        controller.uiState.test {
            val found = awaitFirst { !it.minimized }
            assertFalse("Expected minimized=false after restore() in landscape", found == null)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
