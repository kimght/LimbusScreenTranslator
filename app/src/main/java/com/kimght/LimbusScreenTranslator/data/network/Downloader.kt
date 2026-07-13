package com.kimght.LimbusScreenTranslator.data.network

import java.io.File

interface Downloader {
    suspend fun download(url: String, dest: File, onProgress: (bytesRead: Long) -> Unit = {}): Long
}
