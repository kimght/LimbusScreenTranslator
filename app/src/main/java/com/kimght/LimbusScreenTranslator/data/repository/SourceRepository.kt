package com.kimght.LimbusScreenTranslator.data.repository

import com.kimght.LimbusScreenTranslator.data.db.dao.SourceDao
import com.kimght.LimbusScreenTranslator.data.db.entity.SourceEntity
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.domain.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AddSourceResult {
    data object Success : AddSourceResult
    data object DuplicateName : AddSourceResult
}

@Singleton
class SourceRepository @Inject constructor(
    private val sourceDao: SourceDao,
    private val settings: SettingsRepository,
) {
    val sources: Flow<List<Source>> =
        sourceDao.observeAll().map { list -> list.map { Source(it.name, it.url) } }

    suspend fun seedDefaultsIfEmpty() {
        if (sourceDao.count() == 0) {
            sourceDao.insertAllIfAbsent(DEFAULT_SOURCES.map { SourceEntity(it.name, it.url) })
        }
    }

    suspend fun addSource(name: String, url: String): AddSourceResult {
        val trimmed = name.trim()
        if (sourceDao.get(trimmed) != null) return AddSourceResult.DuplicateName
        sourceDao.insertIfAbsent(SourceEntity(trimmed, url.trim()))
        return AddSourceResult.Success
    }

    suspend fun removeSource(name: String) {
        sourceDao.delete(name)
        val selected = settings.settings.first().selectedBrowsingSource
        if (selected == name) {
            val fallback = sourceDao.getAll().firstOrNull()?.name
            settings.setSelectedBrowsingSource(fallback)
        }
    }

    suspend fun restoreDefaults() {
        sourceDao.replaceAll(DEFAULT_SOURCES.map { SourceEntity(it.name, it.url) })
    }

    suspend fun selectBrowsingSource(name: String) = settings.setSelectedBrowsingSource(name)

    companion object {
        val DEFAULT_SOURCES = listOf(
            Source(
                name = "Github",
                url = "https://gist.githubusercontent.com/kimght/322a2779922ab5a5f96ff7f7dc3f8e82/raw/localizations.json",
            ),
            Source(
                name = "Smallyuan Mirror",
                url = "https://llc.smallyuan.dev/localizations/localizations.json",
            ),
            Source(
                name = "Russian Mirror",
                url = "https://limbus-localizations.duckdns.org/localizations/localizations.json",
            ),
        )
    }
}
