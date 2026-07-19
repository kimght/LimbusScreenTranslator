package com.kimght.limbusscreentranslator.core.di

import android.content.Context
import androidx.room.Room
import com.kimght.limbusscreentranslator.data.db.LimbusDatabase
import com.kimght.limbusscreentranslator.data.db.MIGRATION_1_2
import com.kimght.limbusscreentranslator.data.db.MIGRATION_2_3
import com.kimght.limbusscreentranslator.data.db.dao.ChapterDao
import com.kimght.limbusscreentranslator.data.db.dao.InstalledPackDao
import com.kimght.limbusscreentranslator.data.db.dao.OverlayStateDao
import com.kimght.limbusscreentranslator.data.db.dao.ScenarioDao
import com.kimght.limbusscreentranslator.data.db.dao.SourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LimbusDatabase =
        Room.databaseBuilder(context, LimbusDatabase::class.java, LimbusDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideInstalledPackDao(db: LimbusDatabase): InstalledPackDao = db.installedPackDao()

    @Provides
    fun provideScenarioDao(db: LimbusDatabase): ScenarioDao = db.scenarioDao()

    @Provides
    fun provideSourceDao(db: LimbusDatabase): SourceDao = db.sourceDao()

    @Provides
    fun provideChapterDao(db: LimbusDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideOverlayStateDao(db: LimbusDatabase): OverlayStateDao = db.overlayStateDao()
}
