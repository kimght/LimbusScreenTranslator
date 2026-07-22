package com.kimght.limbusscreentranslator.data.network

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class PreferredIpDnsTest {

    @Test
    fun `lookup returns every unique address in resolver order without a preference`() {
        val first = address(1)
        val second = address(2)
        val dns = PreferredIpDns(
            delegate = fixedDns(first, second, first),
            store = InMemoryPreferredIpStore(),
        )

        assertEquals(listOf(first, second), dns.lookup(HOSTNAME))
    }

    @Test
    fun `lookup moves a current preferred address to the front`() {
        val first = address(1)
        val second = address(2)
        val store = InMemoryPreferredIpStore().apply {
            setPreferredIp(HOSTNAME, checkNotNull(second.hostAddress))
        }
        val dns = PreferredIpDns(fixedDns(first, second), store)

        assertEquals(listOf(second, first), dns.lookup(HOSTNAME))
    }

    @Test
    fun `lookup ignores a preferred address absent from fresh DNS`() {
        val first = address(1)
        val second = address(2)
        val store = InMemoryPreferredIpStore().apply {
            setPreferredIp(HOSTNAME, "203.0.113.9")
        }
        val dns = PreferredIpDns(fixedDns(first, second), store)

        assertEquals(listOf(first, second), dns.lookup(HOSTNAME))
    }

    @Test
    fun `lookup continues when the cache cannot be read`() {
        val first = address(1)
        val store = object : PreferredIpStore {
            override fun getPreferredIp(hostname: String): String? = error("cache unavailable")
            override fun setPreferredIp(hostname: String, ipAddress: String) = Unit
        }
        val dns = PreferredIpDns(fixedDns(first), store)

        assertEquals(listOf(first), dns.lookup(HOSTNAME))
    }

    private fun fixedDns(vararg addresses: InetAddress): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = addresses.toList()
    }

    private fun address(lastOctet: Int): InetAddress = InetAddress.getByAddress(
        HOSTNAME,
        byteArrayOf(127, 0, 0, lastOctet.toByte()),
    )

    private class InMemoryPreferredIpStore : PreferredIpStore {
        private val values = mutableMapOf<String, String>()

        override fun getPreferredIp(hostname: String): String? = values[hostname]

        override fun setPreferredIp(hostname: String, ipAddress: String) {
            values[hostname] = ipAddress
        }
    }

    private companion object {
        const val HOSTNAME = "cdn.example"
    }
}
