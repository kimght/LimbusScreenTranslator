package com.kimght.LimbusScreenTranslator.data.datastore

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SettingsTest {

    @Test
    fun `russian system locale defaults to russian`() {
        assertEquals("ru", Settings.defaultUiLanguage(Locale.forLanguageTag("ru-RU")))
        assertEquals("ru", Settings.defaultUiLanguage(Locale.forLanguageTag("ru")))
    }

    @Test
    fun `any other locale falls back to english`() {
        assertEquals("en", Settings.defaultUiLanguage(Locale.US))
        assertEquals("en", Settings.defaultUiLanguage(Locale.JAPANESE))
        assertEquals("en", Settings.defaultUiLanguage(Locale.forLanguageTag("ko-KR")))
    }
}
