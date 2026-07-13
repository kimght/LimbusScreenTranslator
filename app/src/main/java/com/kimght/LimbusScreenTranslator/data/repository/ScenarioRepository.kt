package com.kimght.LimbusScreenTranslator.data.repository

import com.kimght.LimbusScreenTranslator.data.db.dao.ChapterDao
import com.kimght.LimbusScreenTranslator.data.db.dao.ScenarioDao
import com.kimght.LimbusScreenTranslator.data.db.entity.ChapterEntity
import com.kimght.LimbusScreenTranslator.data.db.entity.ChapterEpisodeEntity
import com.kimght.LimbusScreenTranslator.data.network.LocalizationApi
import com.kimght.LimbusScreenTranslator.domain.model.Chapter
import com.kimght.LimbusScreenTranslator.domain.model.DialogueLine
import com.kimght.LimbusScreenTranslator.domain.model.Episode
import javax.inject.Inject
import javax.inject.Singleton

/** Raised when a selected episode has no scenario in the active pack (spec §13). */
class EpisodeUnavailableException(val episodeCode: String) :
    Exception("No scenario data for episode $episodeCode")

/**
 * Backs the overlay's chapter-select and dialogue reads from Room (the system of record). Lines
 * are returned already resolved and spacer-free, ordered by line index.
 */
@Singleton
class ScenarioRepository @Inject constructor(
    private val scenarioDao: ScenarioDao,
    private val chapterDao: ChapterDao,
    private val api: LocalizationApi,
) {

    /** Fetch and persist the chapter list for [sourceName] from [chaptersUrl]. */
    suspend fun refreshChapters(sourceName: String, chaptersUrl: String) {
        val dto = api.getChapters(chaptersUrl)
        val chapters = ArrayList<ChapterEntity>(dto.chapters.size)
        val episodes = ArrayList<ChapterEpisodeEntity>()
        dto.chapters.forEachIndexed { chapterPos, chapter ->
            chapters += ChapterEntity(
                sourceName = sourceName,
                position = chapterPos,
                name = chapter.name,
                subtitle = chapter.subtitle,
            )
            chapter.episodes.forEachIndexed { episodePos, code ->
                episodes += ChapterEpisodeEntity(
                    sourceName = sourceName,
                    chapterPosition = chapterPos,
                    position = episodePos,
                    episodeCode = code,
                )
            }
        }
        chapterDao.deleteEpisodes(sourceName)
        chapterDao.deleteChapters(sourceName)
        chapterDao.insertChapters(chapters)
        chapterDao.insertEpisodes(episodes)
    }

    /** Factory reset: drop the chapter-select index for every source. */
    suspend fun clearAllChapters() {
        chapterDao.deleteAllEpisodes()
        chapterDao.deleteAllChapters()
    }

    /** Drop the chapter-select index for one source (source removal, spec §2). */
    suspend fun clearChapters(sourceName: String) {
        chapterDao.deleteEpisodes(sourceName)
        chapterDao.deleteChapters(sourceName)
    }

    /** The chapter list for a source, grouped and ordered for the chapter-select view. */
    suspend fun chapters(sourceName: String): List<Chapter> {
        val chapterRows = chapterDao.chaptersFor(sourceName)
        val episodeRows = chapterDao.episodesFor(sourceName).groupBy { it.chapterPosition }
        return chapterRows.map { chapter ->
            Chapter(
                position = chapter.position,
                name = chapter.name,
                subtitle = chapter.subtitle,
                episodes = episodeRows[chapter.position]
                    .orEmpty()
                    .map { Episode(code = it.episodeCode, position = it.position) },
            )
        }
    }

    suspend fun loadEpisode(localizationId: String, episodeCode: String): List<DialogueLine> {
        val rows = scenarioDao.linesFor(localizationId, episodeCode)
        if (rows.isEmpty()) throw EpisodeUnavailableException(episodeCode)
        return rows.map {
            DialogueLine(
                index = it.lineIndex,
                speakerName = it.speakerName,
                title = it.title,
                place = it.place,
                text = it.content,
            )
        }
    }
}
