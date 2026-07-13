package com.kimght.LimbusScreenTranslator.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class OkHttpDownloader @Inject constructor(
    private val client: OkHttpClient,
) : Downloader {

    override suspend fun download(
        url: String,
        dest: File,
        onProgress: (bytesRead: Long) -> Unit,
    ): Long = withContext(Dispatchers.IO) {
        dest.parentFile?.mkdirs()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty response body")
            var total = 0L
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        total += read
                        onProgress(total)
                    }
                    output.flush()
                }
            }
            total
        }
    }

    private companion object {
        const val USER_AGENT = "Limbus Screen Translator"
    }
}
