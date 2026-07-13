package com.kimght.LimbusScreenTranslator.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.install.InstallState
import com.kimght.LimbusScreenTranslator.data.repository.LocalizationRepository
import com.kimght.LimbusScreenTranslator.data.repository.SourceRepository
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.domain.model.PackKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val localization: Localization? = null,
    val status: LocalizationStatus = LocalizationStatus.NOT_INSTALLED,
    val installPercent: Int? = null,
    val installStage: String = "INSTALLING",
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val localizationRepository: LocalizationRepository,
    private val sourceRepository: SourceRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val sourceName: String = checkNotNull(savedStateHandle["sourceName"])
    private val localizationId: String = checkNotNull(savedStateHandle["id"])
    private val packKey: String = PackKey.of(sourceName, localizationId)

    private val localization = MutableStateFlow<Localization?>(null)
    private val loadFailed = MutableStateFlow(false)
    private var chaptersUrl: String? = null

    val uiState: StateFlow<DetailUiState> = combine(
        localization,
        loadFailed,
        localizationRepository.installedPacks,
        localizationRepository.installStates,
        settings.settings,
    ) { loc, failed, installed, installStates, prefs ->
        if (loc == null) {
            DetailUiState(loading = !failed, notFound = failed)
        } else {
            val installedVersion = installed.firstOrNull { it.key == packKey }?.version
            val isInstalling = localizationRepository.isInstalling(packKey)
            val state = installStates[packKey]
            DetailUiState(
                loading = false,
                localization = loc,
                status = LocalizationStatus.of(
                    installedVersion = installedVersion,
                    manifestVersion = loc.version,
                    isActive = prefs.activeLocalizationId == packKey,
                    isInstalling = isInstalling,
                ),
                installPercent = state.percentOrNull(),
                installStage = state.stageLabel(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val source = sourceRepository.sources.first().firstOrNull { it.name == sourceName }
        if (source == null) {
            loadFailed.value = true
            return
        }
        val catalog = runCatching { localizationRepository.fetchCatalog(source.url) }.getOrNull()
        val match = catalog?.localizations?.firstOrNull { it.id == localizationId }
        chaptersUrl = catalog?.chaptersUrl
        if (match == null) loadFailed.value = true else localization.value = match
    }

    fun install() {
        val loc = localization.value ?: return
        viewModelScope.launch { localizationRepository.install(loc, sourceName, chaptersUrl) }
    }

    fun setActive() {
        viewModelScope.launch { localizationRepository.setActive(packKey) }
    }

    fun uninstall(onComplete: () -> Unit) {
        viewModelScope.launch {
            localizationRepository.uninstall(packKey)
            onComplete()
        }
    }
}

private fun InstallState?.percentOrNull(): Int? = when (this) {
    is InstallState.Downloading -> percent
    InstallState.Verifying, InstallState.Extracting, InstallState.Persisting -> 100
    else -> null
}

private fun InstallState?.stageLabel(): String = when (this) {
    is InstallState.Downloading -> "INSTALLING · DOWNLOADING"
    InstallState.Verifying -> "INSTALLING · VERIFYING ZIP"
    InstallState.Extracting -> "INSTALLING · EXTRACTING"
    InstallState.Persisting -> "INSTALLING · SAVING"
    else -> "INSTALLING"
}
