package com.kimght.limbusscreentranslator

import android.app.Application
import com.kimght.limbusscreentranslator.core.di.ApplicationScope
import com.kimght.limbusscreentranslator.data.repository.SourceRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var sourceRepository: SourceRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { sourceRepository.seedDefaultsIfEmpty() }
    }
}
