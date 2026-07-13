package com.kimght.LimbusScreenTranslator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kimght.LimbusScreenTranslator.data.db.entity.ChapterEntity
import com.kimght.LimbusScreenTranslator.data.db.entity.ChapterEpisodeEntity

@Dao
interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<ChapterEpisodeEntity>)

    @Query("DELETE FROM chapter WHERE sourceName = :sourceName")
    suspend fun deleteChapters(sourceName: String)

    @Query("DELETE FROM chapter_episode WHERE sourceName = :sourceName")
    suspend fun deleteEpisodes(sourceName: String)

    @Query("DELETE FROM chapter")
    suspend fun deleteAllChapters()

    @Query("DELETE FROM chapter_episode")
    suspend fun deleteAllEpisodes()

    @Query("SELECT * FROM chapter WHERE sourceName = :sourceName ORDER BY position ASC")
    suspend fun chaptersFor(sourceName: String): List<ChapterEntity>

    @Query(
        "SELECT * FROM chapter_episode WHERE sourceName = :sourceName " +
                "ORDER BY chapterPosition ASC, position ASC",
    )
    suspend fun episodesFor(sourceName: String): List<ChapterEpisodeEntity>
}
