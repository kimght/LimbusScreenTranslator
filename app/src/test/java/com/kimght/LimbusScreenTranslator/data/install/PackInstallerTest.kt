package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.data.network.Downloader
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import com.kimght.LimbusScreenTranslator.testutil.TestZip
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PackInstallerTest {

    private lateinit var cacheRoot: File

    @Before
    fun setUp() {
        cacheRoot = Files.createTempDirectory("cache").toFile()
    }

    @After
    fun tearDown() {
        cacheRoot.deleteRecursively()
    }

    private fun assertNoWorkDirLeftovers() {
        val installDir = File(cacheRoot, "install")
        assertTrue(installDir.listFiles()?.isEmpty() ?: true)
    }

    private class FakeDownloader(private val zipBytes: ByteArray) : Downloader {
        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long {
            dest.parentFile?.mkdirs()
            dest.writeBytes(zipBytes)
            onProgress(zipBytes.size.toLong())
            return zipBytes.size.toLong()
        }
    }

    private class ThrowingDownloader : Downloader {
        override suspend fun download(url: String, dest: File, onProgress: (Long) -> Unit): Long =
            throw java.io.IOException("network down")
    }

    private class RecordingWriter : PackContentWriter {
        var saved: InstalledPack? = null
        var scenarios: List<ScenarioContent> = emptyList()
        var deleted: String? = null
        override suspend fun replacePack(pack: InstalledPack, scenarios: Sequence<ScenarioContent>) {
            this.scenarios = scenarios.toList()
            saved = pack
        }
        override suspend fun deletePack(id: String) {
            deleted = id
        }
    }

    private class ThrowingWriter : PackContentWriter {
        override suspend fun replacePack(pack: InstalledPack, scenarios: Sequence<ScenarioContent>) {
            scenarios.toList()
            throw RuntimeException("disk full")
        }
        override suspend fun deletePack(id: String) = Unit
    }

    private fun localization(format: PackFormat = PackFormat.AUTO, size: Long) = Localization(
        id = "ru-Test",
        version = "v1",
        name = "Test",
        flag = "RU",
        iconUrl = "",
        description = "",
        authors = emptyList(),
        downloadUrl = "https://example.com/p.zip",
        sizeBytes = size,
        format = format,
    )

    private val validZip = TestZip.bytes(
        listOf(
            "MyLang/StoryData/ScenarioModelCodes-AutoCreated.json" to
                """{"dataList":[{"id":"그레고르","name":"Грегор","nickName":"N"}]}""",
            "MyLang/StoryData/S001B.json" to
                """{"dataList":[{"id":0,"content":"line one"},{"id":1},{"id":2,"model":"그레고르","content":"line two"}]}""",
        ),
    )

    @Test
    fun `happy path persists resolved lines, deletes temp files and ends Done`() = runTest {
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader(validZip),
            writer = writer,
            cacheRoot = cacheRoot,
            clock = { 1234L },
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = validZip.size.toLong()), "Github").toList()

        assertTrue(states.last() is InstallState.Done)
        assertTrue(states.any { it is InstallState.Verifying })
        assertTrue(states.any { it is InstallState.Extracting })
        assertTrue(states.any { it is InstallState.Persisting })

        assertEquals(
            InstalledPack(
                id = "ru-Test",
                version = "v1",
                sourceName = "Github",
                installedAt = 1234L,
                name = "Test",
                flag = "RU",
                description = "",
            ),
            writer.saved,
        )
        val scenario = writer.scenarios.single()
        assertEquals("S001B", scenario.code)
        assertEquals(2, scenario.lines.size)
        assertEquals("line one", scenario.lines[0].text)
        assertEquals("Грегор", scenario.lines[1].speakerName)

        assertNoWorkDirLeftovers()
    }

    @Test
    fun `size mismatch fails without persisting`() = runTest {
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader(validZip),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = validZip.size + 1L), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.SIZE_MISMATCH), states.last())
        assertNull(writer.saved)
        assertNoWorkDirLeftovers()
    }

    @Test
    fun `unknown format fails fast`() = runTest {
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader(validZip),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(format = PackFormat.UNKNOWN, size = 1), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.UNKNOWN_FORMAT), states.last())
        assertNull(writer.saved)
    }

    @Test
    fun `download failure is reported and temp dir cleaned`() = runTest {
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = ThrowingDownloader(),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = 10), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.DOWNLOAD_FAILED), states.last())
        assertNoWorkDirLeftovers()
    }

    @Test
    fun `same id from two sources uses separate work dirs and both install`() = runTest {
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader(validZip),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )
        val loc = localization(size = validZip.size.toLong())

        val githubStates = installer.install(loc, "Github").toList()
        val githubSaved = writer.saved
        val mirrorStates = installer.install(loc, "Mirror").toList()
        val mirrorSaved = writer.saved

        assertEquals(InstallState.Done, githubStates.last())
        assertEquals(InstallState.Done, mirrorStates.last())
        assertEquals("Github", githubSaved?.sourceName)
        assertEquals("Mirror", mirrorSaved?.sourceName)
    }

    @Test
    fun `keys that collided under URLEncoder now map to distinct work dirs`() {
        // Under the old `encode(source)-encode(id)` scheme both of these produced "My-pack-x".
        assertNotEquals(
            workDirName("My-pack", "x"),
            workDirName("My", "pack-x"),
        )
    }

    @Test
    fun `corrupt archive fails with EXTRACTION_FAILED`() = runTest {
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader("this is not a zip".toByteArray()),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = 17), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.EXTRACTION_FAILED), states.last())
        assertNull(writer.saved)
        assertNoWorkDirLeftovers()
    }

    @Test
    fun `malformed scenario fails with PARSE_FAILED, not PERSIST_FAILED`() = runTest {
        val badZip = TestZip.bytes(listOf("MyLang/StoryData/S001B.json" to "{ not json"))
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader(badZip),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = badZip.size.toLong()), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.PARSE_FAILED), states.last())
        assertNoWorkDirLeftovers()
    }

    @Test
    fun `writer failure is reported as PERSIST_FAILED`() = runTest {
        val installer = PackInstaller(
            downloader = FakeDownloader(validZip),
            writer = ThrowingWriter(),
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = validZip.size.toLong()), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.PERSIST_FAILED), states.last())
        assertNoWorkDirLeftovers()
    }

    @Test
    fun `archive without StoryData fails to locate language dir`() = runTest {
        val badZip = TestZip.bytes(listOf("MyLang/readme.txt" to "no story data here"))
        val writer = RecordingWriter()
        val installer = PackInstaller(
            downloader = FakeDownloader(badZip),
            writer = writer,
            cacheRoot = cacheRoot,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

        val states = installer.install(localization(size = badZip.size.toLong()), "Github").toList()

        assertEquals(InstallState.Failed(InstallError.LANGUAGE_DIR_NOT_FOUND), states.last())
        assertNull(writer.saved)
    }
}
