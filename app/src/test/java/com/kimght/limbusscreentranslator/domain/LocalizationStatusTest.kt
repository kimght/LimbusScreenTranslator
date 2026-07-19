package com.kimght.limbusscreentranslator.domain

import com.kimght.limbusscreentranslator.domain.model.LocalizationStatus
import com.kimght.limbusscreentranslator.domain.model.hasUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationStatusTest {

    @Test
    fun `hasUpdate is false when not installed`() {
        assertFalse(hasUpdate(installedVersion = null, manifestVersion = "v1"))
    }

    @Test
    fun `hasUpdate is false when versions match`() {
        assertFalse(hasUpdate(installedVersion = "v1", manifestVersion = "v1"))
    }

    @Test
    fun `hasUpdate is true on any string difference`() {
        assertTrue(hasUpdate(installedVersion = "v1", manifestVersion = "v1.0"))
    }

    @Test
    fun `installing wins over everything`() {
        val s = LocalizationStatus.of(
            installedVersion = "v1",
            manifestVersion = "v2",
            isActive = true,
            isInstalling = true,
        )
        assertEquals(LocalizationStatus.INSTALLING, s)
    }

    @Test
    fun `not installed when no recorded version`() {
        val s = LocalizationStatus.of(null, "v1", isActive = false, isInstalling = false)
        assertEquals(LocalizationStatus.NOT_INSTALLED, s)
    }

    @Test
    fun `update available takes precedence over active`() {
        val s = LocalizationStatus.of("v1", "v2", isActive = true, isInstalling = false)
        assertEquals(LocalizationStatus.UPDATE_AVAILABLE, s)
    }

    @Test
    fun `active when current and selected`() {
        val s = LocalizationStatus.of("v1", "v1", isActive = true, isInstalling = false)
        assertEquals(LocalizationStatus.ACTIVE, s)
    }

    @Test
    fun `installed when current but not active`() {
        val s = LocalizationStatus.of("v1", "v1", isActive = false, isInstalling = false)
        assertEquals(LocalizationStatus.INSTALLED, s)
    }
}
