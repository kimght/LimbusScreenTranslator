package com.kimght.limbusscreentranslator.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import kotlinx.coroutines.flow.first
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
class OverlayStateRepositoryTest {

    private lateinit var db: LimbusDatabase
    private lateinit var repo: OverlayStateRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LimbusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = OverlayStateRepository(db.overlayStateDao(), clock = { 42L })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `selecting an episode resets line index and marks viewed`() = runTest {
        repo.selectEpisode("loc", "S001B")

        val state = repo.readingState("loc").first()
        assertEquals("S001B", state.currentEpisode)
        assertNull(state.recentEpisode)
        assertEquals(0, state.lineIndex)
        assertTrue("S001B" in state.viewedEpisodes)
    }

    @Test
    fun `selecting a new episode records previous as recently viewed`() = runTest {
        repo.selectEpisode("loc", "S001B")
        repo.setLineIndex("loc", 7)
        repo.selectEpisode("loc", "S002A")

        val state = repo.readingState("loc").first()
        assertEquals("S002A", state.currentEpisode)
        assertEquals("S001B", state.recentEpisode)
        assertEquals(0, state.lineIndex) // reset on selection
        assertEquals(setOf("S001B", "S002A"), state.viewedEpisodes)
    }

    @Test
    fun `setLineIndex persists within the current episode`() = runTest {
        repo.selectEpisode("loc", "S001B")
        repo.setLineIndex("loc", 12)

        assertEquals(12, repo.readingState("loc").first().lineIndex)
    }

    @Test
    fun `re-selecting the current episode keeps line progress and recent episode`() = runTest {
        repo.selectEpisode("loc", "S001B")
        repo.setLineIndex("loc", 7)
        repo.selectEpisode("loc", "S002A")
        repo.setLineIndex("loc", 4)

        repo.selectEpisode("loc", "S002A") // re-tap the episode already playing

        val state = repo.readingState("loc").first()
        assertEquals("S002A", state.currentEpisode)
        assertEquals("S001B", state.recentEpisode)
        assertEquals(4, state.lineIndex) // progress preserved, not reset to 0
    }

    @Test
    fun `setLineIndex does not resurrect the previous episode`() = runTest {
        repo.selectEpisode("loc", "S001B")
        repo.selectEpisode("loc", "S002A")

        repo.setLineIndex("loc", 9)

        val state = repo.readingState("loc").first()
        assertEquals("S002A", state.currentEpisode)
        assertEquals("S001B", state.recentEpisode)
        assertEquals(9, state.lineIndex)
    }
}
