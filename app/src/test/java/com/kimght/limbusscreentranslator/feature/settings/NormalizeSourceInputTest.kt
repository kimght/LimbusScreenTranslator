package com.kimght.limbusscreentranslator.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeSourceInputTest {

    @Test
    fun `full https url is kept as-is`() {
        val url = "https://example.com/path/localizations.json"
        assertEquals(url, normalizeSourceInput(url, "Example"))
    }

    @Test
    fun `http url is kept as-is`() {
        val url = "http://example.com/manifest.json"
        assertEquals(url, normalizeSourceInput(url, "Example"))
    }

    @Test
    fun `bare host is upgraded to an https manifest url`() {
        assertEquals(
            "https://llc.smallyuan.dev/localizations/localizations.json",
            normalizeSourceInput("llc.smallyuan.dev", "Smallyuan"),
        )
    }

    @Test
    fun `blank host derives a slug from the name`() {
        assertEquals(
            "https://my-mirror.mirror.dev/localizations/localizations.json",
            normalizeSourceInput("   ", "My Mirror"),
        )
    }
}
