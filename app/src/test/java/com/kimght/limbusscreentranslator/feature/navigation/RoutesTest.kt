package com.kimght.limbusscreentranslator.feature.navigation

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoutesTest {

    private fun decodedArgs(route: String): Pair<String, String> {
        val segments = route.removePrefix("detail/").split("/")
        assertEquals(2, segments.size)
        return Uri.decode(segments[0]) to Uri.decode(segments[1])
    }

    @Test
    fun `detail route round-trips source names with spaces`() {
        val (source, id) = decodedArgs(Routes.detail("Smallyuan Mirror", "kr_official"))
        assertEquals("Smallyuan Mirror", source)
        assertEquals("kr_official", id)
    }

    @Test
    fun `detail route round-trips reserved characters`() {
        val (source, id) = decodedArgs(Routes.detail("a/b?c&d+e", "id#1%2"))
        assertEquals("a/b?c&d+e", source)
        assertEquals("id#1%2", id)
    }
}
