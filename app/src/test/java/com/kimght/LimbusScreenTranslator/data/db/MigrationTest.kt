package com.kimght.LimbusScreenTranslator.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LimbusDatabase::class.java,
    )

    @Test
    fun `1 to 2 rewrites pack and child keys and preserves reading progress`() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                "INSERT INTO installed_pack (id, version, sourceName, installedAt) " +
                    "VALUES ('ru-mtl', 'v1', 'Github', 100)",
            )
            execSQL(
                "INSERT INTO scenario_line (localizationId, scenarioCode, lineIndex, content, speakerName, title, place) " +
                    "VALUES ('ru-mtl', 'S001B', 0, 'line', NULL, NULL, NULL)",
            )
            execSQL(
                "INSERT INTO reading_state (localizationId, currentEpisode, recentEpisode, lineIndex) " +
                    "VALUES ('ru-mtl', 'S001B', NULL, 3)",
            )
            execSQL(
                "INSERT INTO episode_progress (localizationId, episodeCode, viewedAt) " +
                    "VALUES ('ru-mtl', 'S001B', 5)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true, MIGRATION_1_2)
        val expectedKey = "Github\u001Fru-mtl"

        db.query("SELECT `key`, id, name, flag, description FROM installed_pack").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedKey, c.getString(0))
            assertEquals("ru-mtl", c.getString(1))
            assertEquals("", c.getString(2))
            assertEquals("", c.getString(3))
            assertEquals("", c.getString(4))
        }
        db.query("SELECT localizationId FROM scenario_line").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expectedKey, c.getString(0))
        }
        db.query(
            "SELECT lineIndex FROM reading_state WHERE localizationId = ?",
            arrayOf(expectedKey),
        ).use { c ->
            assertTrue("reading_state row not rewritten", c.moveToFirst())
            assertEquals(3, c.getInt(0))
        }
        db.query(
            "SELECT viewedAt FROM episode_progress WHERE localizationId = ?",
            arrayOf(expectedKey),
        ).use { c ->
            assertTrue("episode_progress row not rewritten", c.moveToFirst())
            assertEquals(5L, c.getLong(0))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
