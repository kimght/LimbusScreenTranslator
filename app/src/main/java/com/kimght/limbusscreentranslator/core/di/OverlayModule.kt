package com.kimght.limbusscreentranslator.core.di

import com.kimght.limbusscreentranslator.overlay.AndroidOverlaySystemGate
import com.kimght.limbusscreentranslator.overlay.OverlaySystemGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OverlayModule {

    @Binds
    @Singleton
    abstract fun bindOverlaySystemGate(impl: AndroidOverlaySystemGate): OverlaySystemGate
}
