package com.kimght.LimbusScreenTranslator.feature.home

import com.kimght.LimbusScreenTranslator.domain.model.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchSourceCatalogsTest {

    private val sources = listOf(
        Source("Github", "https://a.example/manifest.json"),
        Source("Mirror", "https://b.example/manifest.json"),
        Source("Dead", "https://c.example/manifest.json"),
    )

    @Test
    fun `sources are fetched concurrently, not sequentially`() = runTest {
        val result = fetchSourceCatalogs(sources) { source ->
            delay(1_000)
            source.name.uppercase()
        }

        assertEquals("expected all fetches to overlap", 1_000L, currentTime)
        assertEquals(
            mapOf("Github" to "GITHUB", "Mirror" to "MIRROR", "Dead" to "DEAD"),
            result,
        )
    }

    @Test
    fun `a slow source does not delay the others' results`() = runTest {
        val result = fetchSourceCatalogs(sources) { source ->
            if (source.name == "Dead") delay(30_000) else delay(100)
            source.name
        }

        assertEquals(30_000L, currentTime)
        assertEquals(3, result.size)
    }

    @Test
    fun `failed sources are omitted from the result`() = runTest {
        val result = fetchSourceCatalogs(sources) { source ->
            if (source.name == "Dead") null else source.name
        }

        assertEquals(mapOf("Github" to "Github", "Mirror" to "Mirror"), result)
    }
}
