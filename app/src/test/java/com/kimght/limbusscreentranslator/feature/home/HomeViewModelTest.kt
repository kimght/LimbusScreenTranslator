package com.kimght.limbusscreentranslator.feature.home

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import com.kimght.limbusscreentranslator.data.install.InstallManager
import com.kimght.limbusscreentranslator.data.install.PackInstaller
import com.kimght.limbusscreentranslator.data.install.RoomPackContentWriter
import com.kimght.limbusscreentranslator.data.network.Downloader
import com.kimght.limbusscreentranslator.data.network.LocalizationApi
import com.kimght.limbusscreentranslator.data.repository.CatalogCache
import com.kimght.limbusscreentranslator.data.repository.CatalogFetcher
import com.kimght.limbusscreentranslator.data.repository.ChapterSyncCoordinator
import com.kimght.limbusscreentranslator.data.repository.LocalizationRepository
import com.kimght.limbusscreentranslator.data.repository.ScenarioRepository
import com.kimght.limbusscreentranslator.data.repository.SourceRepository
import com.kimght.limbusscreentranslator.overlay.OverlayRunningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeViewModelTest {

    private lateinit var db: LimbusDatabase
    private lateinit var server: MockWebServer
    private lateinit var cacheRoot: File
    private lateinit var settingsDir: File
    private lateinit var settings: SettingsRepository
    private lateinit var scenarios: ScenarioRepository
    private lateinit var sources: SourceRepository
    private lateinit var repo: LocalizationRepository
    private lateinit var chapterSync: ChapterSyncCoordinator
    private lateinit var runningState: OverlayRunningState
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private class NoopDownloader : Downloader {
        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long = 0L
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer().also { it.start() }
        cacheRoot = Files.createTempDirectory("cache").toFile()
        settingsDir = Files.createTempDirectory("settings").toFile()
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(settingsDir, "settings.preferences_pb") },
        )
        settings = SettingsRepository(dataStore)
        val api = LocalizationApi(OkHttpClient())
        val writer = RoomPackContentWriter(db)
        val installer = PackInstaller(
            downloader = NoopDownloader(),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = Dispatchers.Unconfined,
        )
        scenarios = ScenarioRepository(db, db.scenarioDao(), db.chapterDao(), api)
        sources = SourceRepository(db.sourceDao(), settings)
        val catalogFetcher = CatalogFetcher(api, CatalogCache(now = { 0L }))
        chapterSync = ChapterSyncCoordinator(catalogFetcher, sources, scenarios, now = { 0L })
        repo = LocalizationRepository(
            catalogFetcher = catalogFetcher,
            installedPackDao = db.installedPackDao(),
            installManager = InstallManager(installer, appScope),
            settings = settings,
            contentWriter = writer,
            scenarios = scenarios,
            sources = sources,
            chapterSync = chapterSync,
        )
        runningState = OverlayRunningState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        server.shutdown()
        db.close()
        cacheRoot.deleteRecursively()
        settingsDir.deleteRecursively()
        appScope.cancel()
    }

    private fun manifestJson() =
        """{"localizations":[{"id":"ru-mtl","version":"v1"}],""" +
            """"chapters_url":"${server.url("/chapters.json")}"}"""

    @Test
    fun `refresh syncs the chapter list for sources with a chapters url`() = runTest {
        sources.addSource("Github", server.url("/localizations.json").toString())
        server.enqueue(MockResponse().setBody(manifestJson()))
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}""",
            ),
        )

        HomeViewModel(repo, sources, settings, chapterSync, runningState)

        val chapters = scenarios.observeChapters("Github").first { it.isNotEmpty() }
        assertEquals("Canto I", chapters.single().name)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a failing chapters url does not break the catalog refresh`() = runTest {
        sources.addSource("Github", server.url("/localizations.json").toString())
        server.enqueue(MockResponse().setBody(manifestJson()))
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

        val viewModel = HomeViewModel(repo, sources, settings, chapterSync, runningState)

        assertFalse(viewModel.uiState.first { !it.loading }.loading)
        // 1 manifest request + 3 failed chapter attempts.
        withContext(Dispatchers.Default) {
            while (server.requestCount < 4) Thread.sleep(10)
        }
        assertTrue(scenarios.chapters("Github").isEmpty())
    }
}
