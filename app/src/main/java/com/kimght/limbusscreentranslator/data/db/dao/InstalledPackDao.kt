package com.kimght.limbusscreentranslator.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kimght.limbusscreentranslator.data.db.entity.InstalledPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledPackDao {

    @Query("SELECT * FROM installed_pack")
    fun observeAll(): Flow<List<InstalledPackEntity>>

    @Query("SELECT * FROM installed_pack WHERE `key` = :key")
    suspend fun get(key: String): InstalledPackEntity?

    @Query("SELECT * FROM installed_pack")
    suspend fun getAll(): List<InstalledPackEntity>

    @Query("SELECT * FROM installed_pack WHERE sourceName = :sourceName")
    suspend fun getBySource(sourceName: String): List<InstalledPackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pack: InstalledPackEntity)

    @Query("DELETE FROM installed_pack WHERE `key` = :key")
    suspend fun delete(key: String)
}
