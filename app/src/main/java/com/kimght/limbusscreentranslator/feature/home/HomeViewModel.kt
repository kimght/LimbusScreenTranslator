package com.kimght.limbusscreentranslator.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.install.InstallState
import com.kimght.limbusscreentranslator.data.repository.ChapterSyncCoordinator
import com.kimght.limbusscreentranslator.data.repository.LocalizationRepository
import com.kimght.limbusscreentranslator.data.repository.SourceRepository
import com.kimght.limbusscreentranslator.domain.model.Localization
import com.kimght.limbusscreentranslator.domain.model.Source
import com.kimght.limbusscreentranslator.domain.model.hasUpdate
import com.kimght.limbusscreentranslator.overlay.OverlayRunningState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveLocalization(
    val id: String,
    val name: String,
    val flag: String,
    val installedVersion: String,
    val availableVersion: String,
    val sourceName: String,
    val description: String,
    val updateDescription: String?,
    val hasUpdate: Boolean,
    val isInstalling: Boolean,
)

data class HomeUiState(
    val loading: Boolean = true,
    val active: ActiveLocalization? = null,
    val overlayRunning: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val localizationRepository: LocalizationRepository,
    private val sourceRepository: SourceRepository,
    private val settings: SettingsRepository,
    private val chapterSync: ChapterSyncCoordinator,
    overlayRunningState: OverlayRunningState,
) : ViewModel() {
    private val catalogs = MutableStateFlow<Map<String, Map<String, Localization>>>(emptyMap())
    private val chapterUrls = MutableStateFlow<Map<String, String?>>(emptyMap())

    val uiState: StateFlow<HomeUiState> = combine(
        settings.settings,
        localizationRepository.installedPacks,
        localizationRepository.installStates,
        catalogs,
        overlayRunningState.isRunning,
    ) { prefs, installed, installStates, catalog, overlayRunning ->
        fun offerFor(sourceName: String, id: String): Localization? = catalog[sourceName]?.get(id)

        val activePack = installed.firstOrNull { it.key == prefs.activeLocalizationId }
        val active = activePack?.let { pack ->
            val offer = offerFor(pack.sourceName, pack.id)
            val available = offer?.version ?: pack.version
            ActiveLocalization(
                id = pack.id,
                name = pack.name.ifBlank { pack.id },
                flag = pack.flag.ifBlank { "?" },
                installedVersion = pack.version,
                availableVersion = available,
                sourceName = pack.sourceName,
                description = pack.description,
                updateDescription = offer?.description,
                hasUpdate = hasUpdate(pack.version, available),
                isInstalling = installStates[pack.key].isInstalling(),
            )
        }

        HomeUiState(
            loading = false,
            active = active,
            overlayRunning = overlayRunning,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val sources = sourceRepository.sources.first()
            val fetched = fetchSourceCatalogs(sources) { source ->
                runCatching { localizationRepository.fetchCatalog(source.url) }.getOrNull()
            }
            if (fetched.isNotEmpty()) {
                catalogs.value = fetched.mapValues { (_, catalog) ->
                    catalog.localizations.associateBy { it.id }
                }
                chapterUrls.value = fetched.mapValues { (_, catalog) -> catalog.chaptersUrl }
                fetched.forEach { (sourceName, catalog) ->
                    val chaptersUrl = catalog.chaptersUrl?.takeIf { it.isNotBlank() } ?: return@forEach
                    viewModelScope.launch { chapterSync.sync(sourceName, chaptersUrl) }
                }
            }
        }
    }

    fun updateActive(onDone: () -> Unit) {
        val active = uiState.value.active ?: return onDone()
        if (!active.hasUpdate) return onDone()
        viewModelScope.launch {
            val offer = catalogs.value[active.sourceName]?.get(active.id)
            if (offer != null) {
                localizationRepository.install(
                    offer,
                    active.sourceName,
                    chapterUrls.value[active.sourceName]
                )
            }
            onDone()
        }
    }
}

private fun InstallState?.isInstalling(): Boolean = when (this) {
    null, InstallState.Idle, InstallState.Done, is InstallState.Failed -> false
    else -> true
}

internal suspend fun <T : Any> fetchSourceCatalogs(
    sources: List<Source>,
    fetch: suspend (Source) -> T?,
): Map<String, T> = coroutineScope {
    sources
        .map { source -> async { source.name to fetch(source) } }
        .awaitAll()
        .mapNotNull { (name, catalog) -> catalog?.let { name to it } }
        .toMap()
}
