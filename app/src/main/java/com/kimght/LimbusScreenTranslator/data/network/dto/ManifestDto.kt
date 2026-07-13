package com.kimght.LimbusScreenTranslator.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ManifestDto(
    @SerialName("format_version") val formatVersion: Int = 1,
    val localizations: List<LocalizationDto> = emptyList(),
    @SerialName("chapters_url") val chaptersUrl: String? = null,
)

@Serializable
data class LocalizationDto(
    val id: String,
    val version: String,
    val name: String = "",
    val flag: String = "",
    val icon: String = "",
    val description: String = "",
    val authors: List<String> = emptyList(),
    val url: String = "",
    val size: Long = 0,
    val fonts: List<FontDto> = emptyList(),
    val format: String = "auto",
)

@Serializable
data class FontDto(
    val url: String = "",
    val hash: String = "",
    val name: String = "",
)

@Serializable
data class ChaptersDto(
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
data class ChapterDto(
    val name: String = "",
    val subtitle: String = "",
    val episodes: List<String> = emptyList(),
)
