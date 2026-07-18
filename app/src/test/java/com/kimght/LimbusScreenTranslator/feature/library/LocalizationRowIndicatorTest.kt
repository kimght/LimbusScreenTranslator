package com.kimght.LimbusScreenTranslator.feature.library

import com.composables.icons.lucide.Check
import com.composables.icons.lucide.FolderSync
import com.composables.icons.lucide.Lucide
import com.kimght.LimbusScreenTranslator.R
import com.kimght.LimbusScreenTranslator.domain.model.LocalizationStatus
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus400
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalizationRowIndicatorTest {

    @Test
    fun `not installed is the unmarked default`() {
        assertNull(LocalizationStatus.NOT_INSTALLED.rowIndicator())
    }

    @Test
    fun `installed shows a muted check`() {
        val indicator = LocalizationStatus.INSTALLED.rowIndicator()!!
        assertEquals(R.string.badge_installed, indicator.label)
        assertEquals(Lucide.Check, indicator.icon)
        assertEquals(Limbus500, indicator.tint)
        assertFalse(indicator.spinner)
    }

    @Test
    fun `active shows a gold check`() {
        val indicator = LocalizationStatus.ACTIVE.rowIndicator()!!
        assertEquals(R.string.badge_active, indicator.label)
        assertEquals(Lucide.Check, indicator.icon)
        assertEquals(Limbus300, indicator.tint)
        assertFalse(indicator.spinner)
    }

    @Test
    fun `update available shows an amber folder-sync`() {
        val indicator = LocalizationStatus.UPDATE_AVAILABLE.rowIndicator()!!
        assertEquals(R.string.badge_update, indicator.label)
        assertEquals(Lucide.FolderSync, indicator.icon)
        assertEquals(Limbus400, indicator.tint)
        assertFalse(indicator.spinner)
    }

    @Test
    fun `installing shows a spinner instead of an icon`() {
        val indicator = LocalizationStatus.INSTALLING.rowIndicator()!!
        assertEquals(R.string.badge_installing, indicator.label)
        assertNull(indicator.icon)
        assertEquals(Limbus300, indicator.tint)
        assertTrue(indicator.spinner)
    }
}
