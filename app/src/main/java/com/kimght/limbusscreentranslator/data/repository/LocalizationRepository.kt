package com.kimght.limbusscreentranslator.data.repository

import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.db.dao.InstalledPackDao
import com.kimght.limbusscreentranslator.data.install.InstallManager
import com.kimght.limbusscreentranslator.data.install.InstallState
import com.kimght.limbusscreentranslator.data.install.PackContentWriter
import com.kimght.limbusscreentranslator.domain.model.InstalledPack
import com.kimght.limbusscreentranslator.domain.model.Localization
import com.kimght.limbusscreentranslator.domain.model.LocalizationStatus
import com.kimght.limbusscreentranslator.domain.model.PackKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class LocalizationListing(
    val localization: Localization,
    val packKey: String,
    val installedVersion: String?,
    val status: LocalizationStatus,
)

data class Catalog(
    val localizations: List<Localization>,
    val chaptersUrl: String?,
)

@Singleton
class LocalizationRepository @Inject constructor(
    private val catalogFetcher: CatalogFetcher,
    private val installedPackDao: InstalledPackDao,
    private val installManager: InstallManager,
    private val settings: SettingsRepository,
    private val contentWriter: PackContentWriter,
    private val scenarios: ScenarioRepository,
    private val sources: SourceRepository,
    private val chapterSync: ChapterSyncCoordinator,
) {
    val installedPacks: Flow<List<InstalledPack>> =
        installedPackDao.observeAll().map { list ->
            list.map {
                InstalledPack(
                    id = it.id,
                    version = it.version,
                    sourceName = it.sourceName,
                    installedAt = it.installedAt,
                    name = it.name,
                    flag = it.flag,
                    description = it.description,
                )
            }
        }

    val installStates: Flow<Map<String, InstallState>> = installManager.installStates

    fun isInstalling(id: String): Boolean = installManager.isInstalling(id)

    fun installState(id: String): InstallState = installManager.stateFor(id)

    suspend fun fetchCatalog(manifestUrl: String): Catalog = catalogFetcher.fetch(manifestUrl)

    fun listings(catalog: List<Localization>, sourceName: String): Flow<List<LocalizationListing>> =
        combine(
            installedPackDao.observeAll(),
            settings.settings,
            installManager.installStates,
        ) { installed, prefs, _ ->
            val byKey = installed.associateBy { it.key }
            catalog.map { loc ->
                val key = PackKey.of(sourceName, loc.id)
                val installedVersion = byKey[key]?.version
                LocalizationListing(
                    localization = loc,
                    packKey = key,
                    installedVersion = installedVersion,
                    status = LocalizationStatus.of(
                        installedVersion = installedVersion,
                        manifestVersion = loc.version,
                        isActive = prefs.activeLocalizationId == key,
                        isInstalling = installManager.isInstalling(key),
                    ),
                )
            }
        }

    suspend fun install(
        localization: Localization,
        sourceName: String,
        chaptersUrl: String?,
    ): Boolean {
        val key = PackKey.of(sourceName, localization.id)
        val isFreshInstall = installedPackDao.get(key) == null
        val installed = installManager.install(localization, sourceName)
        if (installed && isFreshInstall) {
            settings.setActiveLocalizationId(key)
        }
        if (installed && !chaptersUrl.isNullOrBlank()) {
            chapterSync.sync(sourceName, chaptersUrl, force = true)
        }
        return installed
    }

    suspend fun setActive(key: String) = settings.setActiveLocalizationId(key)

    suspend fun uninstall(key: String) {
        installManager.cancel(key)
        if (settings.settings.first().activeLocalizationId == key) {
            settings.setActiveLocalizationId(null)
        }
        contentWriter.deletePack(key)
    }

    suspend fun uninstallBySource(sourceName: String) {
        installManager.cancelBySource(sourceName)
        installedPackDao.getBySource(sourceName).forEach { pack ->
            uninstall(PackKey.of(pack.sourceName, pack.id))
        }
        scenarios.clearChapters(sourceName)
    }

    suspend fun uninstallAll() {
        installManager.cancelAll()
        installedPackDao.getAll().forEach { pack ->
            contentWriter.deletePack(PackKey.of(pack.sourceName, pack.id))
        }
        scenarios.clearAllChapters()
        settings.setActiveLocalizationId(null)
    }

    suspend fun removeSourceWithPacks(sourceName: String) {
        uninstallBySource(sourceName)
        sources.removeSource(sourceName)
    }

    suspend fun restoreDefaultSources() {
        uninstallAll()
        sources.restoreDefaults()
    }
}
