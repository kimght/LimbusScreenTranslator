package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.core.di.ApplicationScope
import com.kimght.LimbusScreenTranslator.domain.model.Localization
import com.kimght.LimbusScreenTranslator.domain.model.PackKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallManager @Inject constructor(
    private val installer: PackInstaller,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val jobs = mutableMapOf<String, Deferred<Boolean>>()
    private val states = MutableStateFlow<Map<String, InstallState>>(emptyMap())

    /** Current install state for every pack key with activity since process start. */
    val installStates: StateFlow<Map<String, InstallState>> = states.asStateFlow()

    fun stateFor(key: String): InstallState = states.value[key] ?: InstallState.Idle

    fun isInstalling(key: String): Boolean = when (stateFor(key)) {
        is InstallState.Downloading,
        InstallState.Verifying,
        InstallState.Extracting,
        InstallState.Persisting,
            -> true

        else -> false
    }

    suspend fun install(localization: Localization, sourceName: String): Boolean {
        val key = PackKey.of(sourceName, localization.id)
        val run = mutex.withLock {
            jobs.entries.removeIf { !it.value.isActive }
            jobs[key]?.takeIf { it.isActive } ?: appScope.async {
                var success = false
                installer.install(localization, sourceName).collect { state ->
                    success = state is InstallState.Done
                    states.update { it + (key to state) }
                }
                success
            }.also { jobs[key] = it }
        }
        return run.await()
    }

    suspend fun cancel(key: String) {
        mutex.withLock {
            val job = jobs.remove(key)
            job?.cancelAndJoin()
            states.update { it - key }
        }
    }

    suspend fun cancelBySource(sourceName: String) {
        val prefix = PackKey.of(sourceName, "")
        mutex.withLock {
            val matching = jobs.keys.filter { it.startsWith(prefix) }
                .mapNotNull { key -> jobs.remove(key)?.let { key to it } }
            matching.forEach { (key, job) ->
                job.cancelAndJoin()
                states.update { it - key }
            }
        }
    }

    suspend fun cancelAll() {
        mutex.withLock {
            val running = jobs.values.toList()
            jobs.clear()
            running.forEach { it.cancelAndJoin() }
            states.update { emptyMap() }
        }
    }

    fun clear(key: String) = states.update { it - key }
}
