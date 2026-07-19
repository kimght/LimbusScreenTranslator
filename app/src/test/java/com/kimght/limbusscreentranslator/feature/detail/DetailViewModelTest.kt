package com.kimght.limbusscreentranslator.feature.detail

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
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
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
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
class DetailViewModelTest {

    private lateinit var db: LimbusDatabase
    private lateinit var server: MockWebServer
    private lateinit var cacheRoot: File
    private lateinit var settingsDir: File
    private lateinit var settings: SettingsRepository
    private lateinit var sources: SourceRepository
    private lateinit var repo: LocalizationRepository
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
        val scenarios = ScenarioRepository(db, db.scenarioDao(), db.chapterDao(), api)
        sources = SourceRepository(db.sourceDao(), settings)
        repo = LocalizationRepository(
            api = api,
            installedPackDao = db.installedPackDao(),
            installManager = InstallManager(installer, appScope),
            settings = settings,
            contentWriter = writer,
            scenarios = scenarios,
            sources = sources,
            catalogCache = CatalogCache(now = { 0L }),
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

    @Test
    fun `overlay running state flows into the ui state`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"localizations":[{"id":"ru-mtl","version":"v1"}]}"""),
        )
        sources.addSource("Github", server.url("/localizations.json").toString())
        val viewModel = DetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sourceName" to "Github", "id" to "ru-mtl")),
            localizationRepository = repo,
            sourceRepository = sources,
            settings = settings,
            overlayRunningState = runningState,
        )

        val loaded = viewModel.uiState.first { it.localization != null }
        assertFalse(loaded.overlayRunning)

        runningState.set(true)
        assertTrue(viewModel.uiState.first { it.overlayRunning }.overlayRunning)
    }
}
