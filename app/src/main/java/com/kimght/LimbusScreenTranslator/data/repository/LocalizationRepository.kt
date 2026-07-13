package com.kimght.LimbusScreenTranslator.data.repository

import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.db.dao.InstalledPackDao
import com.kimght.LimbusScreenTranslator.data.install.InstallManager
import com.kimght.LimbusScreenTranslator.data.install.InstallState
import com.kimght.LimbusScreenTranslator.data.install.PackContentWriter
import com.kimght.LimbusScreenTranslator.data.network.LocalizationApi
import com.kimght.LimbusScreenTranslator.data.network.dto.LocalizationDto
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import com.kimght.LimbusScreenTranslator.domain.model.PackKey
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
    private val api: LocalizationApi,
    private val installedPackDao: InstalledPackDao,
    private val installManager: InstallManager,
    private val settings: SettingsRepository,
    private val contentWriter: PackContentWriter,
    private val scenarios: ScenarioRepository,
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

    suspend fun fetchCatalog(manifestUrl: String): Catalog {
        val manifest = api.getManifest(manifestUrl)
        return Catalog(
            localizations = manifest.localizations.map { it.toDomain() },
            chaptersUrl = manifest.chaptersUrl,
        )
    }

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
        val installed = installManager.install(localization, sourceName)
        if (installed && !chaptersUrl.isNullOrBlank()) {
            runCatching { scenarios.refreshChapters(sourceName, chaptersUrl) }
        }
        return installed
    }

    suspend fun setActive(key: String) = settings.setActiveLocalizationId(key)

    suspend fun uninstall(key: String) {
        installManager.cancel(key)
        contentWriter.deletePack(key)
        if (settings.settings.first().activeLocalizationId == key) {
            settings.setActiveLocalizationId(null)
        }
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
}

private fun LocalizationDto.toDomain(): Localization = Localization(
    id = id,
    version = version,
    name = name,
    flag = flag,
    iconUrl = icon,
    description = description,
    authors = authors,
    downloadUrl = url,
    sizeBytes = size,
    format = PackFormat.fromManifest(format),
)
