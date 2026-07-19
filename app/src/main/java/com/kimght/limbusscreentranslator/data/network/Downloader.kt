package com.kimght.limbusscreentranslator.data.network

import java.io.File

interface Downloader {
    suspend fun download(url: String, dest: File, onProgress: (bytesRead: Long) -> Unit = {}): Long
}
