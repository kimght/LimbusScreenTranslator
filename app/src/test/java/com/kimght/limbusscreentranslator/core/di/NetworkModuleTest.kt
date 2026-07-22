package com.kimght.limbusscreentranslator.core.di

import com.kimght.limbusscreentranslator.data.network.PreferredIpStore
import com.kimght.limbusscreentranslator.data.network.SuccessfulIpEventListener
import com.kimght.limbusscreentranslator.data.network.SuccessfulIpInterceptor
import okhttp3.Dns
import okhttp3.Request
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class NetworkModuleTest {

    @Test
    fun `shared client installs provided DNS and successful IP network interceptor`() {
        val dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = emptyList()
        }
        val store = object : PreferredIpStore {
            override fun getPreferredIp(hostname: String): String? = null
            override fun setPreferredIp(hostname: String, ipAddress: String) = Unit
        }

        val client = NetworkModule.provideOkHttpClient(dns, store)
        val call = client.newCall(
            Request.Builder().url("http://example.test/").build()
        )

        assertSame(dns, client.dns)
        assertTrue(
            client.interceptors.single() is SuccessfulIpInterceptor
        )
        assertTrue(
            client.eventListenerFactory.create(call) is SuccessfulIpEventListener
        )
    }
}
