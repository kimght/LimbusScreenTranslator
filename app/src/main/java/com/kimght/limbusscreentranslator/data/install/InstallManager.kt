package com.kimght.limbusscreentranslator.data.install

import com.kimght.limbusscreentranslator.core.di.ApplicationScope
import com.kimght.limbusscreentranslator.domain.model.Localization
import com.kimght.limbusscreentranslator.domain.model.PackKey
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
            val current = jobs[key]
            if (current?.isActive == true) {
                current
            } else {
                appScope.async {
                    current?.let { runCatching { it.join() } }
                    var success = false
                    installer.install(localization, sourceName).collect { state ->
                        success = state is InstallState.Done
                        states.update { it + (key to state) }
                    }
                    success
                }.also { jobs[key] = it }
            }
        }
        return run.await()
    }
    suspend fun cancel(key: String) {
        val job = mutex.withLock { jobs[key] } ?: return
        job.cancelAndJoin()
        mutex.withLock {
            if (jobs[key] === job) {
                jobs.remove(key)
                states.update { it - key }
            }
        }
    }

    suspend fun cancelBySource(sourceName: String) {
        val prefix = PackKey.of(sourceName, "")
        val matching = mutex.withLock {
            jobs.filterKeys { it.startsWith(prefix) }.toList()
        }
        matching.forEach { (key, job) ->
            job.cancelAndJoin()
            mutex.withLock {
                if (jobs[key] === job) {
                    jobs.remove(key)
                    states.update { it - key }
                }
            }
        }
    }

    suspend fun cancelAll() {
        val running = mutex.withLock {
            val snapshot = jobs.values.toList()
            jobs.clear()
            snapshot
        }
        running.forEach { it.cancelAndJoin() }
        states.update { emptyMap() }
    }

    fun clear(key: String) = states.update { it - key }
}
