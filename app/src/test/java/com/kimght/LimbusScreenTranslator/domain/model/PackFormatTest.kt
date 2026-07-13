package com.kimght.LimbusScreenTranslator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PackFormatTest {

    @Test
    fun `known formats map case-insensitively`() {
        assertEquals(PackFormat.AUTO, PackFormat.fromManifest("auto"))
        assertEquals(PackFormat.COMPATIBLE, PackFormat.fromManifest("Compatible"))
        assertEquals(PackFormat.NEW, PackFormat.fromManifest(" NEW "))
    }

    @Test
    fun `unknown format is mapped to UNKNOWN`() {
        assertEquals(PackFormat.UNKNOWN, PackFormat.fromManifest("legacy-xyz"))
        assertEquals(PackFormat.UNKNOWN, PackFormat.fromManifest(""))
    }
}
