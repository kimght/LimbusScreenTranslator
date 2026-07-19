package com.kimght.limbusscreentranslator.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kimght.limbusscreentranslator.data.db.dao.ChapterDao
import com.kimght.limbusscreentranslator.data.db.dao.InstalledPackDao
import com.kimght.limbusscreentranslator.data.db.dao.OverlayStateDao
import com.kimght.limbusscreentranslator.data.db.dao.ScenarioDao
import com.kimght.limbusscreentranslator.data.db.dao.SourceDao
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEntity
import com.kimght.limbusscreentranslator.data.db.entity.ChapterEpisodeEntity
import com.kimght.limbusscreentranslator.data.db.entity.EpisodeProgressEntity
import com.kimght.limbusscreentranslator.data.db.entity.InstalledPackEntity
import com.kimght.limbusscreentranslator.data.db.entity.ReadingStateEntity
import com.kimght.limbusscreentranslator.data.db.entity.ScenarioLineEntity
import com.kimght.limbusscreentranslator.data.db.entity.SourceEntity

/**
 * The app's single Room database — the system of record for installed packs, their parsed
 * scenario lines, chapters, sources, and overlay reading state. Pack files are not kept on disk.
 */
@Database(
    entities = [
        InstalledPackEntity::class,
        SourceEntity::class,
        ScenarioLineEntity::class,
        ChapterEntity::class,
        ChapterEpisodeEntity::class,
        EpisodeProgressEntity::class,
        ReadingStateEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LimbusDatabase : RoomDatabase() {
    abstract fun installedPackDao(): InstalledPackDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun sourceDao(): SourceDao
    abstract fun chapterDao(): ChapterDao
    abstract fun overlayStateDao(): OverlayStateDao

    companion object {
        const val NAME = "limbus.db"
    }
}
