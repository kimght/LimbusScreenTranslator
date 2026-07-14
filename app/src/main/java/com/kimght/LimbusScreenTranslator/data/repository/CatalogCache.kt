package com.kimght.LimbusScreenTranslator.data.repository

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogCache(private val now: () -> Long) {

    @Inject constructor() : this(System::currentTimeMillis)

    private data class Entry(val fetchedAt: Long, val catalog: Catalog)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun get(manifestUrl: String): Catalog? {
        val entry = entries[manifestUrl] ?: return null
        if (now() - entry.fetchedAt > TTL_MS) {
            entries.remove(manifestUrl)
            return null
        }
        return entry.catalog
    }

    fun put(manifestUrl: String, catalog: Catalog) {
        entries[manifestUrl] = Entry(now(), catalog)
    }

    companion object {
        const val TTL_MS = 60_000L
    }
}
