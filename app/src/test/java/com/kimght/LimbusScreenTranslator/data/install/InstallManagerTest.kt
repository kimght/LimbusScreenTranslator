package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.data.network.Downloader
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import com.kimght.LimbusScreenTranslator.domain.model.PackKey
import com.kimght.LimbusScreenTranslator.testutil.TestZip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class InstallManagerTest {

    private val cacheRoot: File = Files.createTempDirectory("cache").toFile()

    private val validZip = TestZip.bytes(
        listOf("MyLang/StoryData/S001B.json" to """{"dataList":[{"id":0,"content":"line one"}]}"""),
    )

    @After
    fun tearDown() {
        cacheRoot.deleteRecursively()
    }

    private class GatedDownloader(private val zipBytes: ByteArray) : Downloader {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long {
            calls++
            gate.await()
            dest.parentFile?.mkdirs()
            dest.writeBytes(zipBytes)
            return zipBytes.size.toLong()
        }
    }

    private class GatedCancelDownloader(private val zipBytes: ByteArray) : Downloader {
        val teardownGate = CompletableDeferred<Unit>()
        val reinstallGate = CompletableDeferred<Unit>()
        private var calls = 0

        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long {
            if (calls++ == 0) {
                try {
                    CompletableDeferred<Unit>().await() // never completes on its own
                } catch (e: CancellationException) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        teardownGate.await()
                    }
                    throw e
                }
            } else {
                reinstallGate.await()
            }
            dest.parentFile?.mkdirs()
            dest.writeBytes(zipBytes)
            return zipBytes.size.toLong()
        }
    }

    private class RecordingWriter : PackContentWriter {
        val saved = mutableListOf<InstalledPack>()
        override suspend fun replacePack(pack: InstalledPack, scenarios: List<ScenarioContent>) {
            saved += pack
        }
        override suspend fun deletePack(id: String) {}
    }

    private fun localization(id: String = "ru-mtl") = Localization(
        id = id,
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

    private fun manager(
        downloader: Downloader,
        writer: PackContentWriter,
        appScope: CoroutineScope,
    ) = InstallManager(
        installer = PackInstaller(
            downloader = downloader,
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = Dispatchers.Unconfined,
        ),
        appScope = appScope,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `concurrent installs for the same pack run the pipeline once`() = runTest {
        val downloader = GatedDownloader(validZip)
        val writer = RecordingWriter()
        val manager = manager(downloader, writer, backgroundScope)

        val first = async { manager.install(localization(), "Github") }
        val second = async { manager.install(localization(), "Github") }
        runCurrent()
        downloader.gate.complete(Unit)

        assertTrue(first.await())
        assertTrue(second.await())
        assertEquals(1, downloader.calls)
        assertEquals(1, writer.saved.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancel stops an in-flight install so nothing is persisted`() = runTest {
        val downloader = GatedDownloader(validZip) // gate never completes
        val writer = RecordingWriter()
        val manager = manager(downloader, writer, backgroundScope)
        val key = PackKey.of("Github", "ru-mtl")

        val caller = async { runCatching { manager.install(localization(), "Github") } }
        runCurrent()
        assertTrue(manager.isInstalling(key))

        manager.cancel(key)

        assertEquals(InstallState.Idle, manager.stateFor(key))
        assertTrue(writer.saved.isEmpty())
        assertTrue(caller.await().exceptionOrNull() is CancellationException)
    }

    @Test
    fun `cancelBySource only cancels that source's installs`() = runTest {
        val downloader = GatedDownloader(validZip)
        val writer = RecordingWriter()
        val manager = manager(downloader, writer, backgroundScope)

        val github = async { runCatching { manager.install(localization(), "Github") } }
        val mirror = async { runCatching { manager.install(localization(), "Mirror") } }
        runCurrent()

        manager.cancelBySource("Github")

        assertEquals(InstallState.Idle, manager.stateFor(PackKey.of("Github", "ru-mtl")))
        assertTrue(manager.isInstalling(PackKey.of("Mirror", "ru-mtl")))
        assertTrue(github.await().exceptionOrNull() is CancellationException)

        downloader.gate.complete(Unit)
        assertEquals(true, mirror.await().getOrNull())
        assertEquals(1, writer.saved.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancel then immediate reinstall starts a fresh pipeline`() = runTest {
        val downloader = GatedCancelDownloader(validZip)
        val writer = RecordingWriter()
        val manager = manager(downloader, writer, backgroundScope)
        val key = PackKey.of("Github", "ru-mtl")

        val first = async { runCatching { manager.install(localization(), "Github") } }
        runCurrent()
        assertTrue(manager.isInstalling(key))

        val cancelJob = async { manager.cancel(key) }
        val second = async { manager.install(localization(), "Github") }
        runCurrent()

        downloader.reinstallGate.complete(Unit)
        runCurrent()

        downloader.teardownGate.complete(Unit)
        advanceUntilIdle()

        cancelJob.await()
        assertTrue(second.await())
        assertTrue(first.await().exceptionOrNull() is CancellationException)
        assertEquals(1, writer.saved.size)
        assertEquals(InstallState.Done, manager.stateFor(key))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelAll stops every in-flight install`() = runTest {
        val downloader = GatedDownloader(validZip)
        val writer = RecordingWriter()
        val manager = manager(downloader, writer, backgroundScope)

        val a = async { runCatching { manager.install(localization(id = "ru-a"), "Github") } }
        val b = async { runCatching { manager.install(localization(id = "ru-b"), "Github") } }
        runCurrent()

        manager.cancelAll()

        assertEquals(emptyMap<String, InstallState>(), manager.installStates.value)
        assertTrue(writer.saved.isEmpty())
        assertTrue(a.await().exceptionOrNull() is CancellationException)
        assertTrue(b.await().exceptionOrNull() is CancellationException)
    }
}
