package com.kimght.limbusscreentranslator.domain.model

data class Localization(
    val id: String,
    val version: String,
    val name: String,
    val flag: String,
    val iconUrl: String,
    val description: String,
    val authors: List<String>,
    val downloadUrl: String,
    val sizeBytes: Long,
    val format: PackFormat,
)
