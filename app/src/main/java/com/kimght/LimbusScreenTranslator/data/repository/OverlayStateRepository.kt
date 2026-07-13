package com.kimght.LimbusScreenTranslator.data.repository

import com.kimght.LimbusScreenTranslator.data.db.dao.OverlayStateDao
import com.kimght.LimbusScreenTranslator.data.db.entity.EpisodeProgressEntity
import com.kimght.LimbusScreenTranslator.data.db.entity.ReadingStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class ReadingState(
    val currentEpisode: String?,
    val recentEpisode: String?,
    val lineIndex: Int,
    val viewedEpisodes: Set<String>,
)

@Singleton
class OverlayStateRepository(
    private val dao: OverlayStateDao,
    private val clock: () -> Long,
) {
    @Inject
    constructor(dao: OverlayStateDao) : this(dao, System::currentTimeMillis)

    fun readingState(localizationId: String): Flow<ReadingState> = combine(
        dao.observeReadingState(localizationId),
        dao.observeViewed(localizationId),
    ) { state, viewed ->
        ReadingState(
            currentEpisode = state?.currentEpisode,
            recentEpisode = state?.recentEpisode,
            lineIndex = state?.lineIndex ?: 0,
            viewedEpisodes = viewed.map { it.episodeCode }.toSet(),
        )
    }

    suspend fun setLineIndex(localizationId: String, lineIndex: Int) {
        val current = dao.getReadingState(localizationId)
        dao.upsertReadingState(
            (current ?: ReadingStateEntity(
                localizationId,
                null,
                null,
                0
            )).copy(lineIndex = lineIndex),
        )
    }

    suspend fun selectEpisode(localizationId: String, episodeCode: String) {
        val previous = dao.getReadingState(localizationId)?.currentEpisode
        dao.upsertReadingState(
            ReadingStateEntity(
                localizationId = localizationId,
                currentEpisode = episodeCode,
                recentEpisode = previous?.takeIf { it != episodeCode },
                lineIndex = 0,
            ),
        )
        dao.markViewed(EpisodeProgressEntity(localizationId, episodeCode, clock()))
    }
}
