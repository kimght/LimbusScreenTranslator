package com.kimght.LimbusScreenTranslator.overlay

import com.kimght.LimbusScreenTranslator.data.datastore.Settings
import com.kimght.LimbusScreenTranslator.domain.model.Chapter
import com.kimght.LimbusScreenTranslator.domain.model.DialogueLine
import com.kimght.LimbusScreenTranslator.domain.model.Episode

enum class OverlayMode { DIALOGUE, CHAPTER, RESIZE }

enum class EpisodeMarker { NONE, VIEWED, NOW_PLAYING }

data class EpisodeCell(
    val code: String,
    val shortLabel: String,
    val marker: EpisodeMarker,
)

data class ChapterRow(
    val key: String,
    val name: String,
    val subtitle: String,
    val progressLabel: String,
    val episodes: List<EpisodeCell>,
    val expanded: Boolean = false,
)

data class OverlaySize(val width: Int, val contentHeight: Int)

data class EpisodeShortcut(
    val code: String,
    val title: String,
    val meta: String,
)

data class EpisodeNav(
    val prev: EpisodeShortcut? = null,
    val next: EpisodeShortcut? = null,
)

data class OverlayUiState(
    val present: Boolean = false,
    val noActiveLocalization: Boolean = false,
    val mode: OverlayMode = OverlayMode.DIALOGUE,
    val minimized: Boolean = false,
    val portrait: Boolean = false,
    val opacity: Float = Settings.DEFAULT_OPACITY,
    val textScale: Float = Settings.DEFAULT_TEXT_SIZE,
    val episodeKey: String? = null,
    val lines: List<DialogueLine> = emptyList(),
    val lineIndex: Int = 0,
    val episodeUnavailable: Boolean = false,
    val chapters: List<ChapterRow> = emptyList(),
    val prevEpisode: EpisodeShortcut? = null,
    val nextEpisode: EpisodeShortcut? = null,
    val chapterContext: String = "",
    val overlayWidth: Int = Settings.DEFAULT_OVERLAY_WIDTH,
    val overlayContentHeight: Int =
        Settings.DEFAULT_OVERLAY_CONTENT_HEIGHT,
) {
    val resizing: Boolean get() = mode == OverlayMode.RESIZE
}

fun lineNumberLabel(index: Int): String = (index + 1).coerceAtLeast(1).toString().padStart(2, '0')

fun buildChapterRows(
    chapters: List<Chapter>,
    currentEpisode: String?,
    viewed: Set<String>,
    expandedChapter: String? = null,
): List<ChapterRow> = chapters.map { chapter ->
    val viewedCount = chapter.episodes.count { it.code in viewed }
    val key = "${chapter.position}-${chapter.name}"
    ChapterRow(
        key = key,
        name = chapter.name,
        subtitle = chapter.subtitle,
        progressLabel = "$viewedCount / ${chapter.episodes.size}",
        expanded = key == expandedChapter,
        episodes = chapter.episodes.map { episode ->
            EpisodeCell(
                code = episode.code,
                shortLabel = (episode.position + 1).toString(),
                marker = when {
                    episode.code == currentEpisode -> EpisodeMarker.NOW_PLAYING
                    episode.code in viewed -> EpisodeMarker.VIEWED
                    else -> EpisodeMarker.NONE
                },
            )
        },
    )
}

fun episodeNav(chapters: List<Chapter>, currentEpisode: String?): EpisodeNav {
    val flat = chapters.flatMap { chapter -> chapter.episodes.map { chapter to it } }
    if (flat.isEmpty()) return EpisodeNav()
    val current = currentEpisode?.let { code -> flat.indexOfFirst { it.second.code == code } } ?: -1
    val prev = if (current > 0) flat[current - 1] else null
    val next = when {
        current < 0 -> flat.first()
        current < flat.size - 1 -> flat[current + 1]
        else -> null
    }
    return EpisodeNav(
        prev = prev?.let { navShortcut(it.first, it.second) },
        next = next?.let { navShortcut(it.first, it.second) },
    )
}

private fun navShortcut(chapter: Chapter, episode: Episode): EpisodeShortcut = EpisodeShortcut(
    code = episode.code,
    title = "Ep ${episode.position + 1}",
    meta = chapter.name,
)

fun chapterContextLabel(totalEpisodes: Int, sourceName: String?): String {
    val episodes = "$totalEpisodes EPISODE" + if (totalEpisodes == 1) "" else "S"
    return if (sourceName.isNullOrBlank()) episodes else "$episodes · $sourceName"
}

fun overlaySizeLabel(width: Int, height: Int): String = "$width × $height"

fun defaultExpandedChapter(chapters: List<Chapter>, currentEpisode: String?): String? {
    val holding = currentEpisode?.let { code ->
        chapters.firstOrNull { ch -> ch.episodes.any { it.code == code } }
    }
    return (holding ?: chapters.firstOrNull())?.let { "${it.position}-${it.name}" }
}
