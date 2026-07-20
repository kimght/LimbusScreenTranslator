package com.kimght.limbusscreentranslator.data.repository

import androidx.room.withTransaction
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import com.kimght.limbusscreentranslator.data.db.dao.ChapterDao
import com.kimght.limbusscreentranslator.data.db.dao.ScenarioDao
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEntity
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEpisodeEntity
import com.kimght.limbusscreentranslator.data.network.LocalizationApi
import com.kimght.limbusscreentranslator.domain.model.Chapter
import com.kimght.limbusscreentranslator.domain.model.DialogueLine
import com.kimght.limbusscreentranslator.domain.model.Episode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class EpisodeUnavailableException(val episodeCode: String) :
    Exception("No scenario data for episode $episodeCode")

@Singleton
class ScenarioRepository @Inject constructor(
    private val db: LimbusDatabase,
    private val scenarioDao: ScenarioDao,
    private val chapterDao: ChapterDao,
    private val api: LocalizationApi,
) {

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
        db.withTransaction {
            chapterDao.deleteEpisodes(sourceName)
            chapterDao.deleteChapters(sourceName)
            chapterDao.insertChapters(chapters)
            chapterDao.insertEpisodes(episodes)
        }
    }

    suspend fun clearAllChapters() {
        db.withTransaction {
            chapterDao.deleteAllEpisodes()
            chapterDao.deleteAllChapters()
        }
    }

    suspend fun clearChapters(sourceName: String) {
        db.withTransaction {
            chapterDao.deleteEpisodes(sourceName)
            chapterDao.deleteChapters(sourceName)
        }
    }

    fun observeChapters(sourceName: String): Flow<List<Chapter>> = combine(
        chapterDao.observeChaptersFor(sourceName),
        chapterDao.observeEpisodesFor(sourceName),
        ::mapChapters,
    )

    suspend fun chapters(sourceName: String): List<Chapter> =
        mapChapters(chapterDao.chaptersFor(sourceName), chapterDao.episodesFor(sourceName))

    private fun mapChapters(
        chapterRows: List<ChapterEntity>,
        episodeRows: List<ChapterEpisodeEntity>,
    ): List<Chapter> {
        val grouped = episodeRows.groupBy { it.chapterPosition }
        return chapterRows.map { chapter ->
            Chapter(
                position = chapter.position,
                name = chapter.name,
                subtitle = chapter.subtitle,
                episodes = grouped[chapter.position]
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
