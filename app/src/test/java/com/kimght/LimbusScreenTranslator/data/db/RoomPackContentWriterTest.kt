package com.kimght.LimbusScreenTranslator.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.LimbusScreenTranslator.data.install.PackParseException
import com.kimght.LimbusScreenTranslator.data.install.RoomPackContentWriter
import com.kimght.LimbusScreenTranslator.data.install.ScenarioContent
import com.kimght.LimbusScreenTranslator.domain.model.DialogueLine
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomPackContentWriterTest {

    private lateinit var db: LimbusDatabase
    private lateinit var writer: RoomPackContentWriter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        writer = RoomPackContentWriter(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun scenario(code: String, vararg texts: String) = ScenarioContent(
        code = code,
        lines = texts.mapIndexed { i, t ->
            DialogueLine(index = i, speakerName = "S$i", title = null, place = null, text = t)
        },
    )

    private fun pack(source: String = "Github") = InstalledPack(
        id = "ru-Test",
        version = "v1",
        sourceName = source,
        installedAt = 100L,
        name = "Тест",
        flag = "RU",
        description = "- fixed typos",
    )

    @Test
    fun `replacePack persists pack metadata and resolved lines under the pack key`() = runTest {
        val pack = pack()
        writer.replacePack(pack, sequenceOf(scenario("S001B", "a", "b")))

        val row = db.installedPackDao().get(pack.key)
        assertEquals("v1", row?.version)
        assertEquals("ru-Test", row?.id)
        assertEquals("Тест", row?.name)
        assertEquals("RU", row?.flag)
        assertEquals("- fixed typos", row?.description)
        val lines = db.scenarioDao().linesFor(pack.key, "S001B")
        assertEquals(2, lines.size)
        assertEquals("a", lines[0].content)
    }

    @Test
    fun `replacePack replaces previous content for the same pack key`() = runTest {
        val pack = pack()
        writer.replacePack(pack, sequenceOf(scenario("S001B", "old1", "old2", "old3")))
        writer.replacePack(pack.copy(version = "v2"), sequenceOf(scenario("S001B", "new1")))

        val lines = db.scenarioDao().linesFor(pack.key, "S001B")
        assertEquals(1, lines.size)
        assertEquals("new1", lines[0].content)
        assertEquals("v2", db.installedPackDao().get(pack.key)?.version)
    }

    @Test
    fun `packs sharing an id but not a source coexist`() = runTest {
        writer.replacePack(pack(source = "Github"), sequenceOf(scenario("S001B", "github")))
        writer.replacePack(pack(source = "Mirror"), sequenceOf(scenario("S001B", "mirror")))

        assertEquals(2, db.installedPackDao().getAll().size)
        assertEquals("github", db.scenarioDao().linesFor(pack("Github").key, "S001B")[0].content)
        assertEquals("mirror", db.scenarioDao().linesFor(pack("Mirror").key, "S001B")[0].content)
    }

    @Test
    fun `a failing scenario stream rolls back and keeps the previous content`() = runTest {
        val pack = pack()
        writer.replacePack(pack, sequenceOf(scenario("S001B", "old")))

        val failing = sequence {
            yield(scenario("S001B", "new"))
            throw PackParseException(RuntimeException("broken file"))
        }
        val result = runCatching { writer.replacePack(pack.copy(version = "v2"), failing) }

        assertTrue(result.isFailure)
        assertEquals("old", db.scenarioDao().linesFor(pack.key, "S001B")[0].content)
        assertEquals("v1", db.installedPackDao().get(pack.key)?.version)
    }

    @Test
    fun `replacePack streams packs larger than one insert chunk`() = runTest {
        val pack = pack()
        val big = ScenarioContent(
            code = "BIG",
            lines = (0 until 2500).map {
                DialogueLine(index = it, speakerName = null, title = null, place = null, text = "t$it")
            },
        )

        writer.replacePack(pack, sequenceOf(big, scenario("S001B", "tail")))

        assertEquals(2500, db.scenarioDao().linesFor(pack.key, "BIG").size)
        assertEquals("tail", db.scenarioDao().linesFor(pack.key, "S001B")[0].content)
    }

    @Test
    fun `deletePack purges only the addressed pack`() = runTest {
        writer.replacePack(pack(source = "Github"), sequenceOf(scenario("S001B", "a")))
        writer.replacePack(pack(source = "Mirror"), sequenceOf(scenario("S001B", "b")))

        writer.deletePack(pack(source = "Github").key)

        assertNull(db.installedPackDao().get(pack(source = "Github").key))
        assertTrue(db.scenarioDao().linesFor(pack(source = "Github").key, "S001B").isEmpty())
        assertEquals("v1", db.installedPackDao().get(pack(source = "Mirror").key)?.version)
    }
}
