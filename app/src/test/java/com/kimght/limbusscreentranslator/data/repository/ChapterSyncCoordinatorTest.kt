package com.kimght.limbusscreentranslator.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import com.kimght.limbusscreentranslator.data.network.LocalizationApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChapterSyncCoordinatorTest {

    private lateinit var db: LimbusDatabase
    private lateinit var server: MockWebServer
    private lateinit var settingsDir: File
    private lateinit var scenarios: ScenarioRepository
    private lateinit var sources: SourceRepository
    private lateinit var coordinator: ChapterSyncCoordinator
    private var nowMs = 0L

    private val chaptersJson =
        """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}"""

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer().also { it.start() }
        settingsDir = Files.createTempDirectory("settings").toFile()
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(settingsDir, "settings.preferences_pb") },
        )
        val api = LocalizationApi(OkHttpClient())
        scenarios = ScenarioRepository(db, db.scenarioDao(), db.chapterDao(), api)
        sources = SourceRepository(db.sourceDao(), SettingsRepository(dataStore))
        coordinator = ChapterSyncCoordinator(
            catalogFetcher = CatalogFetcher(api, CatalogCache(now = { nowMs })),
            sources = sources,
            scenarios = scenarios,
            now = { nowMs },
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
        settingsDir.deleteRecursively()
    }

    @Test
    fun `sync persists the fetched chapter list`() = runTest {
        server.enqueue(MockResponse().setBody(chaptersJson))

        coordinator.sync("Github", server.url("/chapters.json").toString())

        assertEquals(listOf("Canto I"), scenarios.chapters("Github").map { it.name })
    }

    @Test
    fun `sync retries transient failures`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setBody(chaptersJson))

        coordinator.sync("Github", server.url("/chapters.json").toString())

        assertEquals(2, server.requestCount)
        assertEquals(1, scenarios.chapters("Github").size)
    }

    @Test
    fun `sync gives up quietly after exhausting retries`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }

        coordinator.sync("Github", server.url("/chapters.json").toString())

        assertEquals(3, server.requestCount)
        assertTrue(scenarios.chapters("Github").isEmpty())
    }

    @Test
    fun `non-forced sync within the ttl is skipped`() = runTest {
        server.enqueue(MockResponse().setBody(chaptersJson))
        val url = server.url("/chapters.json").toString()

        coordinator.sync("Github", url)
        coordinator.sync("Github", url)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `forced sync bypasses the ttl`() = runTest {
        server.enqueue(MockResponse().setBody(chaptersJson))
        server.enqueue(MockResponse().setBody(chaptersJson))
        val url = server.url("/chapters.json").toString()

        coordinator.sync("Github", url)
        coordinator.sync("Github", url, force = true)

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `non-forced sync refetches once the ttl expires`() = runTest {
        server.enqueue(MockResponse().setBody(chaptersJson))
        server.enqueue(MockResponse().setBody(chaptersJson))
        val url = server.url("/chapters.json").toString()

        coordinator.sync("Github", url)
        nowMs += ChapterSyncCoordinator.TTL_MS + 1
        coordinator.sync("Github", url)

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a failed sync does not start the ttl`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }
        server.enqueue(MockResponse().setBody(chaptersJson))
        val url = server.url("/chapters.json").toString()

        coordinator.sync("Github", url)
        coordinator.sync("Github", url)

        assertEquals(4, server.requestCount)
        assertEquals(1, scenarios.chapters("Github").size)
    }

    @Test
    fun `concurrent syncs for one source share a single fetch`() = runTest {
        val requestReceived = CountDownLatch(1)
        val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestReceived.countDown()
                release.await()
                return MockResponse().setBody(chaptersJson)
            }
        }
        val url = server.url("/chapters.json").toString()

        val first = async(Dispatchers.Default) { coordinator.sync("Github", url, force = true) }
        withContext(Dispatchers.Default) { requestReceived.await() }
        assertEquals(setOf("Github"), coordinator.syncing.value)

        val second = async(Dispatchers.Default) { coordinator.sync("Github", url, force = true) }
        // Give the second sync real time to either join (correct) or issue
        // its own request (bug) before releasing the first.
        withContext(Dispatchers.Default) { Thread.sleep(100) }
        assertEquals(1, server.requestCount)

        release.countDown()
        first.await()
        second.await()

        assertEquals(1, server.requestCount)
        assertTrue(coordinator.syncing.value.isEmpty())
    }

    @Test
    fun `cancelled sync clears the in-flight state`() = runTest {
        val requestReceived = CountDownLatch(1)
        val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestReceived.countDown()
                release.await()
                return MockResponse().setBody(chaptersJson)
            }
        }
        val url = server.url("/chapters.json").toString()

        val job = launch(Dispatchers.Default) { coordinator.sync("Github", url, force = true) }
        withContext(Dispatchers.Default) { requestReceived.await() }
        job.cancel()
        release.countDown()
        job.join()

        assertTrue(coordinator.syncing.value.isEmpty())
    }

    @Test
    fun `syncFromSource rethrows CancellationException from the catalog fetch`() = runTest {
        sources.addSource("Github", "http://example.invalid/localizations.json")
        val cancellingClient = OkHttpClient.Builder()
            .addInterceptor { throw CancellationException("cancelled during fetch") }
            .build()
        val cancellingCoordinator = ChapterSyncCoordinator(
            catalogFetcher = CatalogFetcher(
                LocalizationApi(cancellingClient),
                CatalogCache(now = { nowMs }),
            ),
            sources = sources,
            scenarios = scenarios,
            now = { nowMs },
        )

        val thrown = runCatching {
            cancellingCoordinator.syncFromSource("Github", force = true)
        }.exceptionOrNull()

        assertTrue(
            "expected CancellationException to propagate, but got: $thrown",
            thrown is CancellationException,
        )
    }

    @Test
    fun `syncFromSource resolves the chapters url through the source manifest`() = runTest {
        sources.addSource("Github", server.url("/localizations.json").toString())
        server.enqueue(
            MockResponse().setBody(
                """{"localizations":[{"id":"ru-mtl","version":"v1"}],""" +
                    """"chapters_url":"${server.url("/chapters.json")}"}""",
            ),
        )
        server.enqueue(MockResponse().setBody(chaptersJson))

        coordinator.syncFromSource("Github", force = true)

        assertEquals(1, scenarios.chapters("Github").size)
    }

    @Test
    fun `syncFromSource is a no-op for an unknown source`() = runTest {
        coordinator.syncFromSource("Nope", force = true)

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `syncFromSource is a no-op when the manifest has no chapters url`() = runTest {
        sources.addSource("Github", server.url("/localizations.json").toString())
        server.enqueue(
            MockResponse().setBody("""{"localizations":[{"id":"ru-mtl","version":"v1"}]}"""),
        )

        coordinator.syncFromSource("Github", force = true)

        assertEquals(1, server.requestCount)
        assertTrue(scenarios.chapters("Github").isEmpty())
    }
}
