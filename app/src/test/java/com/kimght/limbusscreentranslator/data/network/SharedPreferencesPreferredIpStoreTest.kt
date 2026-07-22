package com.kimght.limbusscreentranslator.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesPreferredIpStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearPreferences() {
        preferences().edit().clear().commit()
    }

    @Test
    fun `preferred address survives a new store instance`() {
        SharedPreferencesPreferredIpStore(context)
            .setPreferredIp("cdn.example", "192.0.2.4")

        assertEquals(
            "192.0.2.4",
            SharedPreferencesPreferredIpStore(context).getPreferredIp("cdn.example"),
        )
    }

    @Test
    fun `malformed preference type is treated as a cache miss`() {
        preferences().edit().putInt("cdn.example", 4).commit()

        assertNull(SharedPreferencesPreferredIpStore(context).getPreferredIp("cdn.example"))
    }

    private fun preferences() = context.getSharedPreferences(
        SharedPreferencesPreferredIpStore.PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
}
