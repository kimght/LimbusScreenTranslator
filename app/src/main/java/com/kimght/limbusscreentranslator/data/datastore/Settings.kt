package com.kimght.limbusscreentranslator.data.datastore

import java.util.Locale

data class Settings(
    val opacity: Float = DEFAULT_OPACITY,
    val textSize: Float = DEFAULT_TEXT_SIZE,
    val uiLanguage: String = defaultUiLanguage(),
    // Null means "never saved" — (0,0) is a legitimate top-left position.
    val panelX: Int? = null,
    val panelY: Int? = null,
    val minimizedX: Int? = null,
    val minimizedY: Int? = null,
    val overlayWidth: Int = DEFAULT_OVERLAY_WIDTH,
    val overlayContentHeight: Int = DEFAULT_OVERLAY_CONTENT_HEIGHT,
    val activeLocalizationId: String? = null,
    val selectedBrowsingSource: String? = null,
) {
    companion object {
        const val MIN_OPACITY = 0.2f
        const val MAX_OPACITY = 1.0f
        const val DEFAULT_OPACITY = 0.85f

        const val MIN_TEXT_SIZE = 0.7f
        const val MAX_TEXT_SIZE = 1.6f
        const val DEFAULT_TEXT_SIZE = 1.0f

        fun defaultUiLanguage(locale: Locale = Locale.getDefault()): String =
            if (locale.language == "ru") "ru" else "en"

        const val MIN_OVERLAY_WIDTH = 260
        const val MAX_OVERLAY_WIDTH = 560
        const val DEFAULT_OVERLAY_WIDTH = 360

        const val MIN_OVERLAY_CONTENT_HEIGHT = 110
        const val MAX_OVERLAY_CONTENT_HEIGHT = 300
        const val DEFAULT_OVERLAY_CONTENT_HEIGHT = 150
    }
}
