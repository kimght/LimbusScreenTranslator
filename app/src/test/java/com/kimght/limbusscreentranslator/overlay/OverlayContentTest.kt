package com.kimght.limbusscreentranslator.overlay

import com.kimght.limbusscreentranslator.domain.model.Chapter
import com.kimght.limbusscreentranslator.domain.model.Episode
import com.kimght.limbusscreentranslator.overlay.ui.NavButtonPart
import com.kimght.limbusscreentranslator.overlay.ui.navButtonParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayContentTest {

    private val chapters = listOf(
        Chapter(
            position = 0,
            name = "CANTO VII",
            subtitle = "The Dream Ending",
            episodes = listOf(Episode("7-1", 0), Episode("7-2", 1), Episode("7-3", 2), Episode("7-4", 3)),
        ),
        Chapter(
            position = 1,
            name = "INTERVALLO",
            subtitle = "An Uninvited Guest",
            episodes = listOf(Episode("I-1", 0), Episode("I-2", 1)),
        ),
    )

    @Test
    fun `line label is one-based and zero-padded`() {
        assertEquals("01", lineNumberLabel(0))
        assertEquals("10", lineNumberLabel(9))
        assertEquals("100", lineNumberLabel(99))
    }

    @Test
    fun `negative line index never produces a zero or negative label`() {
        assertEquals("01", lineNumberLabel(-3))
    }

    @Test
    fun `chapter rows carry per-chapter progress and per-episode markers`() {
        val rows = buildChapterRows(chapters, currentEpisode = "7-4", viewed = setOf("7-1", "7-2", "7-3"))

        assertEquals("3 / 4", rows[0].progressLabel)
        assertEquals("0 / 2", rows[1].progressLabel)
        assertEquals(EpisodeMarker.VIEWED, rows[0].episodes[0].marker)
        assertEquals(EpisodeMarker.NOW_PLAYING, rows[0].episodes[3].marker)
        assertEquals(EpisodeMarker.NONE, rows[1].episodes[0].marker)
        assertEquals("4", rows[0].episodes[3].shortLabel)
    }

    @Test
    fun `episode nav returns flattened neighbours of the current episode`() {
        val nav = episodeNav(chapters, currentEpisode = "7-2")

        assertEquals("7-1", nav.prev?.code)
        assertEquals(1, nav.prev?.episodeNumber)
        assertEquals("CANTO VII", nav.prev?.meta)
        assertEquals("7-3", nav.next?.code)
        assertEquals(3, nav.next?.episodeNumber)
    }

    @Test
    fun `episode nav has no previous at the start of the list`() {
        val nav = episodeNav(chapters, currentEpisode = "7-1")

        assertNull(nav.prev)
        assertEquals("7-2", nav.next?.code)
    }

    @Test
    fun `episode nav has no next at the end of the list`() {
        val nav = episodeNav(chapters, currentEpisode = "I-2")

        assertEquals("I-1", nav.prev?.code)
        assertNull(nav.next)
    }

    @Test
    fun `episode nav crosses chapter boundaries`() {
        val nav = episodeNav(chapters, currentEpisode = "7-4")

        assertEquals("I-1", nav.next?.code)
        assertEquals(1, nav.next?.episodeNumber)
        assertEquals("INTERVALLO", nav.next?.meta)
    }

    @Test
    fun `episode nav points next at the first episode when none is current`() {
        val nav = episodeNav(chapters, currentEpisode = null)

        assertNull(nav.prev)
        assertEquals("7-1", nav.next?.code)
    }

    @Test
    fun `episode nav is empty when there are no episodes`() {
        val nav = episodeNav(emptyList(), currentEpisode = null)

        assertNull(nav.prev)
        assertNull(nav.next)
    }

    @Test
    fun `quick navigation mirrors labels around destination text`() {
        assertEquals(
            listOf(NavButtonPart.LABEL, NavButtonPart.DESTINATION),
            navButtonParts(destinationFirst = false),
        )
        assertEquals(
            listOf(NavButtonPart.DESTINATION, NavButtonPart.LABEL),
            navButtonParts(destinationFirst = true),
        )
    }

    @Test
    fun `overlay size label joins width and height`() {
        assertEquals("360 × 150", overlaySizeLabel(360, 150))
    }

    @Test
    fun `default expanded chapter is the one holding the current episode`() {
        assertEquals("1-INTERVALLO", defaultExpandedChapter(chapters, currentEpisode = "I-2"))
    }

    @Test
    fun `default expanded chapter falls back to the first when none current`() {
        assertEquals("0-CANTO VII", defaultExpandedChapter(chapters, currentEpisode = null))
        assertNull(defaultExpandedChapter(emptyList(), currentEpisode = null))
    }

    @Test
    fun `only the expanded chapter is marked expanded`() {
        val rows = buildChapterRows(
            chapters, currentEpisode = "7-4", viewed = emptySet(), expandedChapter = "1-INTERVALLO",
        )
        assertEquals(false, rows[0].expanded)
        assertEquals(true, rows[1].expanded)
    }
}
