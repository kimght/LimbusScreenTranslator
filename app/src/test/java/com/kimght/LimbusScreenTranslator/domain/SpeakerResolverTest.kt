package com.kimght.LimbusScreenTranslator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeakerResolverTest {

    private val table = mapOf(
        "그레고르" to ModelInfo(name = "Грегор", nickName = "Грешник №13"),
        "유리" to ModelInfo(name = "Юри", nickName = ""),
    )

    @Test
    fun `explicit teller and title take priority over model table`() {
        val r = SpeakerResolver.resolve(
            teller = "Override Name",
            title = "Override Title",
            model = "그레고르",
            modelTable = table,
        )
        assertEquals("Override Name", r.name)
        assertEquals("Override Title", r.title)
    }

    @Test
    fun `model code resolves name and title from the table when not explicit`() {
        val r = SpeakerResolver.resolve(teller = null, title = null, model = "그레고르", modelTable = table)
        assertEquals("Грегор", r.name)
        assertEquals("Грешник №13", r.title)
    }

    @Test
    fun `empty mapped nickName resolves to a null title`() {
        val r = SpeakerResolver.resolve(teller = null, title = null, model = "유리", modelTable = table)
        assertEquals("Юри", r.name)
        assertNull(r.title)
    }

    @Test
    fun `no model and no teller is narration`() {
        val r = SpeakerResolver.resolve(teller = null, title = null, model = null, modelTable = table)
        assertNull(r.name)
        assertNull(r.title)
    }

    @Test
    fun `blank teller falls back to the model table`() {
        val r = SpeakerResolver.resolve(teller = "  ", title = null, model = "그레고르", modelTable = table)
        assertEquals("Грегор", r.name)
    }

    @Test
    fun `unknown model code yields no speaker`() {
        val r = SpeakerResolver.resolve(teller = null, title = null, model = "missing", modelTable = table)
        assertNull(r.name)
        assertNull(r.title)
    }
}
