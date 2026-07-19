package com.kimght.limbusscreentranslator.feature.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimght.limbusscreentranslator.R
import com.kimght.limbusscreentranslator.data.datastore.Settings
import com.kimght.limbusscreentranslator.data.datastore.SettingsRepository
import com.kimght.limbusscreentranslator.data.repository.AddSourceResult
import com.kimght.limbusscreentranslator.data.repository.LocalizationRepository
import com.kimght.limbusscreentranslator.data.repository.SourceRepository
import com.kimght.limbusscreentranslator.domain.model.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class UiLanguage(val code: String, val label: String)

data class UiMessage(@StringRes val id: Int, val args: List<Any> = emptyList())

data class SettingsUiState(
    val opacity: Float = Settings.DEFAULT_OPACITY,
    val uiLanguage: String = Settings.defaultUiLanguage(),
    val sources: List<Source> = emptyList(),
) {
    val opacityPercent: Int get() = (opacity * 100).roundToInt()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val localizations: LocalizationRepository,
) : ViewModel() {

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.settings,
        sourceRepository.sources,
    ) { prefs, sources ->
        SettingsUiState(
            opacity = prefs.opacity,
            uiLanguage = prefs.uiLanguage
                .takeIf { code -> UI_LANGUAGES.any { it.code == code } }
                ?: Settings.defaultUiLanguage(),
            sources = sources,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setOpacityPercent(percent: Int) {
        viewModelScope.launch { settings.setOpacity(percent / 100f) }
    }

    fun setUiLanguage(code: String) {
        viewModelScope.launch { settings.setUiLanguage(code) }
    }

    fun addSource(name: String, hostOrUrl: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val url = normalizeSourceInput(hostOrUrl, trimmedName)
        viewModelScope.launch {
            when (sourceRepository.addSource(trimmedName, url)) {
                AddSourceResult.Success -> _messages.tryEmit(
                    UiMessage(R.string.settings_msg_source_added, listOf(trimmedName))
                )
                AddSourceResult.DuplicateName -> _messages.tryEmit(
                    UiMessage(R.string.settings_msg_source_exists)
                )
            }
        }
    }

    fun removeSource(name: String) {
        viewModelScope.launch {
            localizations.removeSourceWithPacks(name)
            _messages.tryEmit(UiMessage(R.string.settings_msg_source_removed, listOf(name)))
        }
    }

    fun resetEverything() {
        viewModelScope.launch {
            localizations.restoreDefaultSources()
            settings.resetToDefaults()
            _messages.tryEmit(UiMessage(R.string.settings_msg_reset_done))
        }
    }

    companion object {
        val UI_LANGUAGES = listOf(
            UiLanguage("en", "English"),
            UiLanguage("ru", "Русский"),
        )
    }
}

internal fun normalizeSourceInput(hostOrUrl: String, name: String): String {
    val raw = hostOrUrl.trim()
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith(
            "https://",
            ignoreCase = true
        )
    ) {
        return raw
    }
    val host = raw.ifBlank {
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + ".mirror.dev"
    }
    return "https://$host/localizations/localizations.json"
}
