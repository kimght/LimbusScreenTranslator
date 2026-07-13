package com.kimght.LimbusScreenTranslator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kimght.LimbusScreenTranslator.data.db.entity.ScenarioLineEntity

@Dao
interface ScenarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<ScenarioLineEntity>)

    @Query("DELETE FROM scenario_line WHERE localizationId = :localizationId")
    suspend fun deleteForLocalization(localizationId: String)

    @Query(
        "SELECT * FROM scenario_line WHERE localizationId = :localizationId " +
                "AND scenarioCode = :scenarioCode ORDER BY lineIndex ASC",
    )
    suspend fun linesFor(localizationId: String, scenarioCode: String): List<ScenarioLineEntity>

    @Query(
        "SELECT COUNT(*) FROM scenario_line WHERE localizationId = :localizationId " +
                "AND scenarioCode = :scenarioCode",
    )
    suspend fun lineCount(localizationId: String, scenarioCode: String): Int
}
