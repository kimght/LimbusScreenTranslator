package com.kimght.limbusscreentranslator.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesPreferredIpStore @Inject constructor(
    @ApplicationContext context: Context,
) : PreferredIpStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getPreferredIp(hostname: String): String? = try {
        preferences.getString(hostname, null)
    } catch (_: RuntimeException) {
        null
    }

    override fun setPreferredIp(hostname: String, ipAddress: String) {
        runCatching {
            preferences.edit().putString(hostname, ipAddress).apply()
        }
    }

    companion object {
        internal const val PREFERENCES_NAME = "preferred_ip_routes"
    }
}
