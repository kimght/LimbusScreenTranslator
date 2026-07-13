package com.kimght.LimbusScreenTranslator.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimght.LimbusScreenTranslator.data.datastore.SettingsRepository
import com.kimght.LimbusScreenTranslator.data.install.InstallState
import com.kimght.LimbusScreenTranslator.data.repository.LocalizationListing
import com.kimght.LimbusScreenTranslator.data.repository.LocalizationRepository
import com.kimght.LimbusScreenTranslator.data.repository.SourceRepository
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryItem(
    val listing: LocalizationListing,
    val installPercent: Int?,
)

data class LibraryUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val noSources: Boolean = false,
    val sources: List<Source> = emptyList(),
    val selectedSource: Source? = null,
    val items: List<LibraryItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localizationRepository: LocalizationRepository,
    private val sourceRepository: SourceRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private data class LoadedCatalog(val source: Source, val localizations: List<Localization>)

    private val catalog = MutableStateFlow<LoadedCatalog?>(null)

    private val chaptersUrl = MutableStateFlow<String?>(null)
    private val fetchError = MutableStateFlow(false)
    private val selectedSourceName = MutableStateFlow<String?>(null)

    private val selectedSource: StateFlow<Source?> = combine(
        sourceRepository.sources,
        selectedSourceName,
        settings.settings,
    ) { sources, explicit, prefs ->
        val name = explicit ?: prefs.selectedBrowsingSource
        sources.firstOrNull { it.name == name } ?: sources.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<LibraryUiState> =
        catalog.flatMapLatest { cat ->
            if (cat == null) {
                combine(sourceRepository.sources, selectedSource, fetchError) { sources, sel, err ->
                    if (sources.isEmpty()) {
                        LibraryUiState(loading = false, noSources = true)
                    } else {
                        LibraryUiState(
                            loading = !err,
                            error = err,
                            sources = sources,
                            selectedSource = sel
                        )
                    }
                }
            } else {
                combine(
                    sourceRepository.sources,
                    selectedSource,
                    localizationRepository.listings(cat.localizations, cat.source.name),
                    localizationRepository.installStates,
                ) { sources, sel, listings, installStates ->
                    if (sources.isEmpty()) {
                        LibraryUiState(loading = false, noSources = true)
                    } else {
                        LibraryUiState(
                            loading = false,
                            error = false,
                            sources = sources,
                            selectedSource = sel,
                            items = listings.map { listing ->
                                LibraryItem(listing, installStates[listing.packKey].percentOrNull())
                            },
                        )
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch {
            selectedSource.collect { source -> if (source != null) loadCatalog(source) }
        }
    }

    private suspend fun loadCatalog(source: Source) {
        catalog.value = null
        fetchError.value = false
        runCatching { localizationRepository.fetchCatalog(source.url) }
            .onSuccess {
                catalog.value = LoadedCatalog(source, it.localizations)
                chaptersUrl.value = it.chaptersUrl
            }
            .onFailure { fetchError.value = true }
    }

    fun selectSource(name: String) {
        selectedSourceName.value = name
        viewModelScope.launch { sourceRepository.selectBrowsingSource(name) }
    }

    fun retry() {
        viewModelScope.launch { selectedSource.first()?.let { loadCatalog(it) } }
    }

    fun install(item: LibraryItem) {
        val loaded = catalog.value ?: return
        viewModelScope.launch {
            localizationRepository.install(
                item.listing.localization,
                loaded.source.name,
                chaptersUrl.value
            )
        }
    }
}

private fun InstallState?.percentOrNull(): Int? = when (this) {
    is InstallState.Downloading -> percent
    InstallState.Verifying, InstallState.Extracting, InstallState.Persisting -> 100
    else -> null
}
