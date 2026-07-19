package com.kimght.limbusscreentranslator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PackKeyTest {

    @Test
    fun `key joins source and id with the unit separator`() {
        assertEquals("Github\u001Fru-mtl", PackKey.of("Github", "ru-mtl"))
    }

    @Test
    fun `distinct source-id pairs never collide`() {
        assertNotEquals(PackKey.of("A", "b-c"), PackKey.of("A-b", "c"))
        assertNotEquals(PackKey.of("Github", "ru-mtl"), PackKey.of("Mirror", "ru-mtl"))
    }

    @Test
    fun `installed pack derives its key from source and id`() {
        val pack = InstalledPack("ru-mtl", "v1", "Github", 100L)
        assertEquals(PackKey.of("Github", "ru-mtl"), pack.key)
    }
}
