package com.kimght.limbusscreentranslator.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import com.kimght.limbusscreentranslator.data.db.dao.ChapterDao
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEpisodeEntity
import com.kimght.limbusscreentranslator.data.install.ScenarioContent
import com.kimght.limbusscreentranslator.data.install.RoomPackContentWriter
import com.kimght.limbusscreentranslator.data.network.LocalizationApi
import com.kimght.limbusscreentranslator.domain.model.DialogueLine
import com.kimght.limbusscreentranslator.domain.model.InstalledPack
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScenarioRepositoryTest {

    private lateinit var db: LimbusDatabase
    private lateinit var server: MockWebServer
    private lateinit var repo: ScenarioRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer().also { it.start() }
        repo = ScenarioRepository(db, db.scenarioDao(), db.chapterDao(), LocalizationApi(OkHttpClient()))
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun `refreshChapters parses and stores chapters in order`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"chapters":[
                  {"name":"Canto I","subtitle":"sub","episodes":["S001B","S001A"]},
                  {"name":"Canto II","subtitle":"sub2","episodes":["S002B"]}
                ]}
                """.trimIndent(),
            ),
        )

        repo.refreshChapters("Github", server.url("/chapters.json").toString())

        val chapters = repo.chapters("Github")
        assertEquals(2, chapters.size)
        assertEquals("Canto I", chapters[0].name)
        assertEquals(listOf("S001B", "S001A"), chapters[0].episodes.map { it.code })
        assertEquals(listOf("S002B"), chapters[1].episodes.map { it.code })
    }

    @Test
    fun `refreshChapters rolls back so a mid-write failure keeps the existing index`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto I","subtitle":"s","episodes":["S001B"]}]}""",
            ),
        )
        repo.refreshChapters("Github", server.url("/chapters.json").toString())
        assertEquals(1, repo.chapters("Github").size)

        // A refresh whose episode insert fails must roll back its own deletes, not empty the index.
        val failingChapterDao = object : ChapterDao by db.chapterDao() {
            override suspend fun insertEpisodes(episodes: List<ChapterEpisodeEntity>) {
                throw RuntimeException("boom")
            }
        }
        val failing = ScenarioRepository(db, db.scenarioDao(), failingChapterDao, LocalizationApi(OkHttpClient()))
        server.enqueue(
            MockResponse().setBody(
                """{"chapters":[{"name":"Canto II","subtitle":"s2","episodes":["S002B"]}]}""",
            ),
        )

        assertThrows(RuntimeException::class.java) {
            kotlinx.coroutines.runBlocking {
                failing.refreshChapters("Github", server.url("/chapters2.json").toString())
            }
        }

        val chapters = repo.chapters("Github")
        assertEquals(1, chapters.size)
        assertEquals("Canto I", chapters[0].name)
        assertEquals(listOf("S001B"), chapters[0].episodes.map { it.code })
    }

    @Test
    fun `loadEpisode returns ordered lines from the database`() = runTest {
        val pack = InstalledPack("loc", "v1", "Github", 0L)
        RoomPackContentWriter(db).replacePack(
            pack,
            sequenceOf(
                ScenarioContent(
                    "S001B",
                    listOf(
                        DialogueLine(0, "A", null, null, "first"),
                        DialogueLine(1, null, null, null, "second"),
                    ),
                ),
            ),
        )

        val lines = repo.loadEpisode(pack.key, "S001B")

        assertEquals(listOf("first", "second"), lines.map { it.text })
        assertEquals("A", lines[0].speakerName)
    }

    @Test
    fun `loadEpisode throws when scenario is missing`() = runTest {
        assertThrows(EpisodeUnavailableException::class.java) {
            kotlinx.coroutines.runBlocking { repo.loadEpisode("loc", "MISSING") }
        }
    }
}
