package com.kimght.LimbusScreenTranslator.core.di

import android.content.Context
import com.kimght.LimbusScreenTranslator.data.install.PackContentWriter
import com.kimght.LimbusScreenTranslator.data.install.PackInstaller
import com.kimght.LimbusScreenTranslator.data.install.RoomPackContentWriter
import com.kimght.LimbusScreenTranslator.data.network.Downloader
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
