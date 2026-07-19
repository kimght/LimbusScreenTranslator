package com.kimght.limbusscreentranslator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kimght.limbusscreentranslator.data.db.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM source ORDER BY name ASC")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM source")
    suspend fun getAll(): List<SourceEntity>

    @Query("SELECT * FROM source WHERE name = :name")
    suspend fun get(name: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(source: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(sources: List<SourceEntity>)

    @Query("DELETE FROM source WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM source")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(sources: List<SourceEntity>) {
        deleteAll()
        insertAllIfAbsent(sources)
    }

    @Query("SELECT COUNT(*) FROM source")
    suspend fun count(): Int
}
