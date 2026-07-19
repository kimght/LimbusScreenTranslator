package com.kimght.limbusscreentranslator.core.di

import android.content.Context
import com.kimght.limbusscreentranslator.data.install.PackContentWriter
import com.kimght.limbusscreentranslator.data.install.PackInstaller
import com.kimght.limbusscreentranslator.data.install.RoomPackContentWriter
import com.kimght.limbusscreentranslator.data.network.Downloader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InstallModule {

    @Provides
    @Singleton
    fun providePackInstaller(
        @ApplicationContext context: Context,
        downloader: Downloader,
        writer: PackContentWriter,
    ): PackInstaller = PackInstaller(
        downloader = downloader,
        writer = writer,
        cacheRoot = context.cacheDir,
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InstallBindsModule {

    @Binds
    @Singleton
    abstract fun bindPackContentWriter(impl: RoomPackContentWriter): PackContentWriter
}
