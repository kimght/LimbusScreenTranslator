package com.kimght.LimbusScreenTranslator.domain.model

data class Chapter(
    val position: Int,
    val name: String,
    val subtitle: String,
    val episodes: List<Episode>,
)

data class Episode(
    val code: String,
    val position: Int,
)
