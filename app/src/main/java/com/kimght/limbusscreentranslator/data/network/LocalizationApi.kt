package com.kimght.limbusscreentranslator.data.network

import com.kimght.limbusscreentranslator.data.network.dto.ChaptersDto
import com.kimght.limbusscreentranslator.data.network.dto.ManifestDto
import com.kimght.limbusscreentranslator.data.serialization.PackParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

class LocalizationApi @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun getManifest(url: String): ManifestDto = PackParser.parseManifest(getString(url))

    suspend fun getChapters(url: String): ChaptersDto = PackParser.parseChapters(getString(url))

    private suspend fun getString(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Request failed: HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    private companion object {
        const val USER_AGENT = "Limbus Screen Translator"
    }
}
