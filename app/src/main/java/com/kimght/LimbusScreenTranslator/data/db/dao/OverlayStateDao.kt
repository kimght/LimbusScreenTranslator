package com.kimght.LimbusScreenTranslator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kimght.LimbusScreenTranslator.data.db.entity.EpisodeProgressEntity
import com.kimght.LimbusScreenTranslator.data.db.entity.ReadingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OverlayStateDao {

    @Query("SELECT * FROM reading_state WHERE localizationId = :localizationId")
    fun observeReadingState(localizationId: String): Flow<ReadingStateEntity?>

    @Query("SELECT * FROM reading_state WHERE localizationId = :localizationId")
    suspend fun getReadingState(localizationId: String): ReadingStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingState(state: ReadingStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadingStateIfAbsent(state: ReadingStateEntity)

    @Query("UPDATE reading_state SET lineIndex = :lineIndex WHERE localizationId = :localizationId")
    suspend fun updateLineIndex(localizationId: String, lineIndex: Int)

    @Query("DELETE FROM reading_state WHERE localizationId = :localizationId")
    suspend fun deleteReadingState(localizationId: String)

    @Query("SELECT * FROM episode_progress WHERE localizationId = :localizationId")
    fun observeViewed(localizationId: String): Flow<List<EpisodeProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markViewed(progress: EpisodeProgressEntity)

    @Query("DELETE FROM episode_progress WHERE localizationId = :localizationId")
    suspend fun deleteProgress(localizationId: String)
}
