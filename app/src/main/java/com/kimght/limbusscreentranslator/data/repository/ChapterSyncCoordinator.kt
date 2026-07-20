package com.kimght.limbusscreentranslator.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ChapterSyncCoordinator(
    private val catalogFetcher: CatalogFetcher,
    private val sources: SourceRepository,
    private val scenarios: ScenarioRepository,
    private val now: () -> Long,
) {
    @Inject constructor(
        catalogFetcher: CatalogFetcher,
        sources: SourceRepository,
        scenarios: ScenarioRepository,
    ) : this(catalogFetcher, sources, scenarios, System::currentTimeMillis)

    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val lastSuccess = mutableMapOf<String, Long>()
    private val syncingFlow = MutableStateFlow<Set<String>>(emptySet())

    val syncing: StateFlow<Set<String>> = syncingFlow.asStateFlow()

    suspend fun syncFromSource(sourceName: String, force: Boolean = false) {
        val source = sources.sources.first().firstOrNull { it.name == sourceName } ?: return
        val chaptersUrl = fetchChaptersUrl(source.url)?.takeIf { it.isNotBlank() } ?: return
        sync(sourceName, chaptersUrl, force)
    }

    private suspend fun fetchChaptersUrl(manifestUrl: String): String? =
        try {
            catalogFetcher.fetch(manifestUrl).chaptersUrl
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    suspend fun sync(sourceName: String, chaptersUrl: String, force: Boolean = false) {
        var owned = false
        val attempt = mutex.withLock {
            if (!force && withinTtl(sourceName)) return
            inFlight[sourceName] ?: CompletableDeferred<Unit>().also {
                inFlight[sourceName] = it
                syncingFlow.value = syncingFlow.value + sourceName
                owned = true
            }
        }
        if (!owned) {
            attempt.join()
            return
        }
        try {
            refreshWithRetry(sourceName, chaptersUrl)
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    inFlight.remove(sourceName)
                    syncingFlow.value = syncingFlow.value - sourceName
                }
            }
            attempt.complete(Unit)
        }
    }

    private suspend fun refreshWithRetry(sourceName: String, chaptersUrl: String) {
        var delayMs = INITIAL_BACKOFF_MS
        repeat(ATTEMPTS) { attempt ->
            val result = runCatching { scenarios.refreshChapters(sourceName, chaptersUrl) }
            if (result.isSuccess) {
                mutex.withLock { lastSuccess[sourceName] = now() }
                return
            }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (attempt < ATTEMPTS - 1) {
                delay(delayMs.milliseconds)
                delayMs *= 2
            }
        }
    }

    private fun withinTtl(sourceName: String): Boolean {
        val at = lastSuccess[sourceName] ?: return false
        return now() - at < TTL_MS
    }

    companion object {
        const val ATTEMPTS = 3
        const val INITIAL_BACKOFF_MS = 500L
        const val TTL_MS = 60 * 60 * 1000L
    }
}
