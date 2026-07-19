package com.kimght.limbusscreentranslator.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val OPACITY = floatPreferencesKey("opacity")
        val TEXT_SIZE = floatPreferencesKey("text_size")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val PANEL_X = intPreferencesKey("panel_x")
        val PANEL_Y = intPreferencesKey("panel_y")
        val MINIMIZED_X = intPreferencesKey("minimized_x")
        val MINIMIZED_Y = intPreferencesKey("minimized_y")
        val OVERLAY_WIDTH = intPreferencesKey("overlay_width")
        val OVERLAY_CONTENT_HEIGHT = intPreferencesKey("overlay_content_height")
        val ACTIVE_LOCALIZATION = stringPreferencesKey("active_localization_id")
        val BROWSING_SOURCE = stringPreferencesKey("selected_browsing_source")
    }

    val settings: Flow<Settings> = dataStore.data.catch { e ->
        // A power-off mid-write can leave preferences_pb unparseable. Fall back to
        // defaults so every collector keeps working instead of crashing on launch.
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs ->
        Settings(
            opacity = prefs[Keys.OPACITY] ?: Settings.DEFAULT_OPACITY,
            textSize = prefs[Keys.TEXT_SIZE] ?: Settings.DEFAULT_TEXT_SIZE,
            uiLanguage = prefs[Keys.UI_LANGUAGE] ?: Settings.defaultUiLanguage(),
            panelX = prefs[Keys.PANEL_X],
            panelY = prefs[Keys.PANEL_Y],
            minimizedX = prefs[Keys.MINIMIZED_X],
            minimizedY = prefs[Keys.MINIMIZED_Y],
            overlayWidth = prefs[Keys.OVERLAY_WIDTH] ?: Settings.DEFAULT_OVERLAY_WIDTH,
            overlayContentHeight = prefs[Keys.OVERLAY_CONTENT_HEIGHT]
                ?: Settings.DEFAULT_OVERLAY_CONTENT_HEIGHT,
            activeLocalizationId = prefs[Keys.ACTIVE_LOCALIZATION],
            selectedBrowsingSource = prefs[Keys.BROWSING_SOURCE],
        )
    }

    suspend fun setOpacity(value: Float) = edit {
        it[Keys.OPACITY] = value.coerceIn(Settings.MIN_OPACITY, Settings.MAX_OPACITY)
    }

    suspend fun setTextSize(value: Float) = edit {
        it[Keys.TEXT_SIZE] = value.coerceIn(Settings.MIN_TEXT_SIZE, Settings.MAX_TEXT_SIZE)
    }

    suspend fun setUiLanguage(language: String) = edit { it[Keys.UI_LANGUAGE] = language }

    suspend fun setPanelPosition(x: Int, y: Int) = edit {
        it[Keys.PANEL_X] = x
        it[Keys.PANEL_Y] = y
    }

    suspend fun setMinimizedPosition(x: Int, y: Int) = edit {
        it[Keys.MINIMIZED_X] = x
        it[Keys.MINIMIZED_Y] = y
    }

    suspend fun setOverlaySize(width: Int, height: Int) = edit {
        it[Keys.OVERLAY_WIDTH] =
            width.coerceIn(Settings.MIN_OVERLAY_WIDTH, Settings.MAX_OVERLAY_WIDTH)
        it[Keys.OVERLAY_CONTENT_HEIGHT] =
            height.coerceIn(
                Settings.MIN_OVERLAY_CONTENT_HEIGHT,
                Settings.MAX_OVERLAY_CONTENT_HEIGHT
            )
    }

    suspend fun setActiveLocalizationId(id: String?) = edit {
        if (id == null) it.remove(Keys.ACTIVE_LOCALIZATION) else it[Keys.ACTIVE_LOCALIZATION] = id
    }

    suspend fun setSelectedBrowsingSource(name: String?) = edit {
        if (name == null) it.remove(Keys.BROWSING_SOURCE) else it[Keys.BROWSING_SOURCE] = name
    }

    suspend fun resetToDefaults() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
