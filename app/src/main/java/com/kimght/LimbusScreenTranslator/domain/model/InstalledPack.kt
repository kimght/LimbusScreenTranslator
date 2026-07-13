package com.kimght.LimbusScreenTranslator.domain.model

data class InstalledPack(
    val id: String,
    val version: String,
    val sourceName: String,
    val installedAt: Long,
    val name: String = "",
    val flag: String = "",
    val description: String = "",
) {
    val key: String get() = PackKey.of(sourceName, id)
}
