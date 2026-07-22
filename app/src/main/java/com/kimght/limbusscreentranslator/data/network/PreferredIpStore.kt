package com.kimght.limbusscreentranslator.data.network

interface PreferredIpStore {
    fun getPreferredIp(hostname: String): String?
    fun setPreferredIp(hostname: String, ipAddress: String)
}
