package com.kimght.LimbusScreenTranslator.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.db.LimbusDatabase
import com.kimght.LimbusScreenTranslator.data.install.InstallManager
import com.kimght.LimbusScreenTranslator.data.install.PackInstaller
import com.kimght.LimbusScreenTranslator.data.install.RoomPackContentWriter
import com.kimght.LimbusScreenTranslator.data.network.Downloader
import com.kimght.LimbusScreenTranslator.data.network.LocalizationApi
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import com.kimght.LimbusScreenTranslator.domain.model.PackKey
import com.kimght.LimbusScreenTranslator.testutil.TestZip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class LocalizationRepositoryTest {

    private lateinit var db: LimbusDatabase
    private lateinit var server: MockWebServer
    private lateinit var cacheRoot: File
    private lateinit var settingsDir: File
    private lateinit var settings: SettingsRepository
    private lateinit var scenarios: ScenarioRepository
    private lateinit var sources: SourceRepository
    private lateinit var repo: LocalizationRepository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val validZip = TestZip.bytes(
        listOf(
            "MyLang/StoryData/S001B.json" to """{"dataList":[{"id":0,"content":"line one"}]}""",
        ),
    )

    private class FakeDownloader(private val zipBytes: ByteArray) : Downloader {
        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long {
            dest.parentFile?.mkdirs()
            dest.writeBytes(zipBytes)
            onProgress(zipBytes.size.toLong())
            return zipBytes.size.toLong()
        }
    }

    private lateinit var installManager: InstallManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer().also { it.start() }
        cacheRoot = Files.createTempDirectory("cache").toFile()
        settingsDir = Files.createTempDirectory("settings").toFile()
        buildRepo(FakeDownloader(validZip))
    }

    private fun buildRepo(downloader: Downloader) {
        val api = LocalizationApi(OkHttpClient())
        val writer = RoomPackContentWriter(db)
        val installer = PackInstaller(
            downloader = downloader,
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = Dispatchers.Unconfined,
        )
        installManager = InstallManager(installer, appScope)
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(settingsDir, "settings.preferences_pb") },
        )
        settings = SettingsRepository(dataStore)
        scenarios = ScenarioRepository(db, db.scenarioDao(), db.chapterDao(), api)
        sources = SourceRepository(db.sourceDao(), settings)
        repo = LocalizationRepository(
            api = api,
            installedPackDao = db.installedPackDao(),
            installManager = installManager,
            settings = settings,
            contentWriter = writer,
            scenarios = scenarios,
            sources = sources,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
        cacheRoot.deleteRecursively()
        settingsDir.deleteRecursively()
        appScope.cancel()
    }

    private fun localization() = Localization(
        id = "ru-mtl",
        version = "v1",
        name = "MTL",
        flag = "RU",
        iconUrl = "",
        description = "",
        authors = emptyList(),
        downloadUrl = "https://example.com/p.zip",
        sizeBytes = validZip.size.toLong(),
        format = PackFormat.AUTO,
    )

    @Test
    fun `install persists the source chapter list so the overlay has episodes`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B","S001A"]}]}""",
            ),
        )

        val ok = repo.install(localization(), "Github", server.url("/chapters.json").toString())

        assertTrue(ok)
        val chapters = scenarios.chapters("Github")
        assertEquals(1, chapters.size)
        assertEquals("Canto I", chapters[0].name)
        assertEquals(listOf("S001B", "S001A"), chapters[0].episodes.map { it.code })
    }

    @Test
    fun `install retries a transient chapters fetch failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}""",
            ),
        )

        val ok = repo.install(localization(), "Github", server.url("/chapters.json").toString())

        assertTrue(ok)
        val chapters = scenarios.chapters("Github")
        assertEquals(1, chapters.size)
        assertEquals(listOf("S001B"), chapters[0].episodes.map { it.code })
    }

    @Test
    fun `install still succeeds when there is no chapters url`() = runTest {
        val ok = repo.install(localization(), "Github", chaptersUrl = null)

        assertTrue(ok)
        assertTrue(scenarios.chapters("Github").isEmpty())
    }

    @Test
    fun `fetchCatalog surfaces the manifest chapters_url`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"localizations":[{"id":"ru-mtl","version":"v1"}],"chapters_url":"https://example.com/chapters.json"}""",
            ),
        )

        val catalog = repo.fetchCatalog(server.url("/localizations.json").toString())

        assertEquals("https://example.com/chapters.json", catalog.chaptersUrl)
        assertEquals(listOf("ru-mtl"), catalog.localizations.map { it.id })
    }

    @Test
    fun `fetchCatalog drops manifest entries with a blank id`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"localizations":[{"id":"","version":"v1"},{"id":"ru-mtl","version":"v1"}]}""",
            ),
        )

        val catalog = repo.fetchCatalog(server.url("/localizations.json").toString())

        assertEquals(listOf("ru-mtl"), catalog.localizations.map { it.id })
    }

    private val githubKey = PackKey.of("Github", "ru-mtl")

    @Test
    fun `uninstallAll purges packs, chapters, and the active selection`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}""",
            ),
        )
        repo.install(localization(), "Github", server.url("/chapters.json").toString())
        repo.setActive(githubKey)

        repo.uninstallAll()

        assertTrue(repo.installedPacks.first().isEmpty())
        assertTrue(scenarios.chapters("Github").isEmpty())
        assertNull(settings.settings.first().activeLocalizationId)
    }

    @Test
    fun `same id from two sources installs as two independent packs`() = runTest {
        repo.install(localization(), "Github", chaptersUrl = null)
        repo.install(localization(), "Mirror", chaptersUrl = null)

        val packs = repo.installedPacks.first()
        assertEquals(2, packs.size)
        assertEquals(setOf("Github", "Mirror"), packs.map { it.sourceName }.toSet())
        assertEquals("MTL", packs[0].name)
        assertEquals("RU", packs[0].flag)
    }

    @Test
    fun `uninstall addresses one source's pack and keeps the other's active`() = runTest {
        repo.install(localization(), "Github", chaptersUrl = null)
        repo.install(localization(), "Mirror", chaptersUrl = null)
        val mirrorKey = PackKey.of("Mirror", "ru-mtl")
        repo.setActive(mirrorKey)

        repo.uninstall(githubKey)

        val packs = repo.installedPacks.first()
        assertEquals(listOf("Mirror"), packs.map { it.sourceName })
        assertEquals(mirrorKey, settings.settings.first().activeLocalizationId)
    }

    @Test
    fun `uninstallBySource purges only that source's packs and chapters`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}""",
            ),
        )
        repo.install(localization(), "Github", server.url("/chapters.json").toString())
        repo.install(localization(), "Mirror", chaptersUrl = null)
        repo.setActive(githubKey)

        repo.uninstallBySource("Github")

        val packs = repo.installedPacks.first()
        assertEquals(listOf("Mirror"), packs.map { it.sourceName })
        assertTrue(scenarios.chapters("Github").isEmpty())
        assertNull(settings.settings.first().activeLocalizationId)
    }

    @Test
    fun `uninstall cancels an in-flight install so it cannot commit after the purge`() = runTest {
        settingsDir.deleteRecursively()
        settingsDir = Files.createTempDirectory("settings").toFile()
        val gate = CompletableDeferred<Unit>()
        buildRepo(object : Downloader {
            override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long {
                gate.await()
                dest.parentFile?.mkdirs()
                dest.writeBytes(validZip)
                return validZip.size.toLong()
            }
        })

        val installing = async {
            runCatching { repo.install(localization(), "Github", chaptersUrl = null) }
        }
        yield()
        assertTrue(repo.isInstalling(githubKey))

        repo.uninstall(githubKey)
        gate.complete(Unit) // too late — the pipeline was cancelled and joined

        assertTrue(installing.await().exceptionOrNull() is CancellationException)
        assertTrue(repo.installedPacks.first().isEmpty())
    }

    @Test
    fun `removeSourceWithPacks uninstalls the source's packs and drops the source`() = runTest {
        sources.addSource("Github", "https://example.com/localizations.json")
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}""",
            ),
        )
        repo.install(localization(), "Github", server.url("/chapters.json").toString())
        repo.install(localization(), "Mirror", chaptersUrl = null)
        repo.setActive(githubKey)

        repo.removeSourceWithPacks("Github")

        val packs = repo.installedPacks.first()
        assertEquals(listOf("Mirror"), packs.map { it.sourceName })
        assertTrue(scenarios.chapters("Github").isEmpty())
        assertNull(settings.settings.first().activeLocalizationId)
        assertTrue(sources.sources.first().none { it.name == "Github" })
    }

    @Test
    fun `listings are keyed to the browsing source`() = runTest {
        repo.install(localization(), "Github", chaptersUrl = null)

        val githubListings = repo.listings(listOf(localization()), "Github").first()
        val mirrorListings = repo.listings(listOf(localization()), "Mirror").first()

        assertEquals(LocalizationStatus.INSTALLED, githubListings[0].status)
        assertEquals(githubKey, githubListings[0].packKey)
        assertEquals(LocalizationStatus.NOT_INSTALLED, mirrorListings[0].status)
    }
}
