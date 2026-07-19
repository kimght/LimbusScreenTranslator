package com.kimght.limbusscreentranslator.core.i18n

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kimght.limbusscreentranslator.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalizedContextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `ru context resolves russian strings`() {
        assertEquals("Библиотека", context.localizedTo("ru").getString(R.string.nav_library))
    }

    @Test
    fun `en context resolves english strings`() {
        assertEquals("Library", context.localizedTo("en").getString(R.string.nav_library))
    }

    @Test
    fun `format args are applied`() {
        assertEquals(
            "Источник добавлен · X",
            context.localizedTo("ru").getString(R.string.settings_msg_source_added, "X"),
        )
    }

    @Test
    fun `russian plurals pick correct forms`() {
        val res = context.localizedTo("ru").resources
        assertEquals("1 ЭПИЗОД", res.getQuantityString(R.plurals.overlay_episode_count, 1, 1))
        assertEquals("3 ЭПИЗОДА", res.getQuantityString(R.plurals.overlay_episode_count, 3, 3))
        assertEquals("6 ЭПИЗОДОВ", res.getQuantityString(R.plurals.overlay_episode_count, 6, 6))
    }

    // hiltViewModel() finds the host Activity by unwrapping ContextWrapper.baseContext
    // from LocalContext — the wrapper must keep the original context in that chain.
    @Test
    fun `wrapper keeps the base context reachable for hiltViewModel`() {
        assertSame(context, context.localizedWrapper("ru").baseContext)
    }

    @Test
    fun `wrapper resolves strings in the requested language`() {
        assertEquals("Библиотека", context.localizedWrapper("ru").getString(R.string.nav_library))
        assertEquals("Library", context.localizedWrapper("en").getString(R.string.nav_library))
    }
}
