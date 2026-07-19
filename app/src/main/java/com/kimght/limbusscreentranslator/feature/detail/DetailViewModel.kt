package com.kimght.limbusscreentranslator.feature.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.install.InstallState
import com.kimght.limbusscreentranslator.data.repository.LocalizationRepository
import com.kimght.limbusscreentranslator.data.repository.SourceRepository
import com.kimght.limbusscreentranslator.domain.model.Localization
import com.kimght.limbusscreentranslator.domain.model.LocalizationStatus
import com.kimght.limbusscreentranslator.domain.model.PackKey
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
    @StringRes val installStage: Int = R.string.install_stage_installing,
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
                installStage = state.stageLabelRes(),
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

@StringRes
internal fun InstallState?.stageLabelRes(): Int = when (this) {
    is InstallState.Downloading -> R.string.install_stage_downloading
    InstallState.Verifying -> R.string.install_stage_verifying
    InstallState.Extracting -> R.string.install_stage_extracting
    InstallState.Persisting -> R.string.install_stage_saving
    else -> R.string.install_stage_installing
}
