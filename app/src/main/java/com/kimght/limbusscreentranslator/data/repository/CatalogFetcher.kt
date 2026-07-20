package com.kimght.limbusscreentranslator.data.repository

import com.kimght.limbusscreentranslator.data.network.LocalizationApi
import com.kimght.limbusscreentranslator.data.network.dto.LocalizationDto
import com.kimght.limbusscreentranslator.domain.model.Localization
import com.kimght.limbusscreentranslator.domain.model.PackFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogFetcher @Inject constructor(
    private val api: LocalizationApi,
    private val cache: CatalogCache,
) {
    suspend fun fetch(manifestUrl: String): Catalog {
        cache.get(manifestUrl)?.let { return it }
        val manifest = api.getManifest(manifestUrl)
        return Catalog(
            localizations = manifest.localizations
                .filter { it.id.isNotBlank() }
                .map { it.toDomain() },
            chaptersUrl = manifest.chaptersUrl,
        ).also { cache.put(manifestUrl, it) }
    }
}

private fun LocalizationDto.toDomain(): Localization = Localization(
    id = id,
    version = version,
    name = name,
    flag = flag,
    iconUrl = icon,
    description = description,
    authors = authors,
    downloadUrl = url,
    sizeBytes = size,
    format = PackFormat.fromManifest(format),
)
