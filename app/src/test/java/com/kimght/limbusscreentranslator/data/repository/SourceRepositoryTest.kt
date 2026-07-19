package com.kimght.limbusscreentranslator.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SourceRepositoryTest {

    private lateinit var db: LimbusDatabase
    private lateinit var settingsDir: File
    private lateinit var repo: SourceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDir = Files.createTempDirectory("settings").toFile()
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(settingsDir, "settings.preferences_pb") },
        )
        repo = SourceRepository(db.sourceDao(), SettingsRepository(dataStore))
    }

    @After
    fun tearDown() {
        db.close()
        settingsDir.deleteRecursively()
    }

    @Test
    fun `seedDefaultsIfEmpty populates the built-ins and is idempotent`() = runTest {
        repo.seedDefaultsIfEmpty()
        assertEquals(
            SourceRepository.DEFAULT_SOURCES.map { it.name }.toSet(),
            repo.sources.first().map { it.name }.toSet(),
        )

        repo.seedDefaultsIfEmpty()
        assertEquals(SourceRepository.DEFAULT_SOURCES.size, repo.sources.first().size)
    }

    @Test
    fun `seedDefaultsIfEmpty leaves an existing non-empty table untouched`() = runTest {
        repo.addSource("Custom", "https://custom.dev/localizations.json")

        repo.seedDefaultsIfEmpty()

        assertEquals(listOf("Custom"), repo.sources.first().map { it.name })
    }

    @Test
    fun `restoreDefaults replaces custom sources with the built-ins`() = runTest {
        repo.seedDefaultsIfEmpty()
        repo.addSource("Custom", "https://custom.dev/localizations/localizations.json")
        repo.removeSource("Github")

        repo.restoreDefaults()

        val names = repo.sources.first().map { it.name }.toSet()
        assertEquals(SourceRepository.DEFAULT_SOURCES.map { it.name }.toSet(), names)
    }
}
