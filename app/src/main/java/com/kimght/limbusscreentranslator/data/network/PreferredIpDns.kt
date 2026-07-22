package com.kimght.limbusscreentranslator.data.network

import okhttp3.Dns
import java.net.InetAddress

class PreferredIpDns(
    private val delegate: Dns,
    private val store: PreferredIpStore,
) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname).distinct()
        val preferredIp = runCatching { store.getPreferredIp(hostname) }.getOrNull()
            ?: return addresses
        val preferredIndex = addresses.indexOfFirst { it.hostAddress == preferredIp }
        if (preferredIndex <= 0) return addresses

        return buildList(addresses.size) {
            add(addresses[preferredIndex])
            addresses.forEachIndexed { index, address ->
                if (index != preferredIndex) add(address)
            }
        }
    }
}
