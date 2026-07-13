package com.kimght.LimbusScreenTranslator

import android.app.Application
import com.kimght.LimbusScreenTranslator.core.di.ApplicationScope
import com.kimght.LimbusScreenTranslator.data.repository.SourceRepository
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
        // A fresh install boots with an empty source table; seed the built-in sources
        // so Home/Library/Detail have something to browse without manual setup.
        applicationScope.launch { sourceRepository.seedDefaultsIfEmpty() }
    }
}
