package com.kimght.limbusscreentranslator.data.install

import com.kimght.limbusscreentranslator.domain.model.DialogueLine
import com.kimght.limbusscreentranslator.domain.model.InstalledPack

data class ScenarioContent(
    val code: String,
    val lines: List<DialogueLine>,
)

/** Thrown lazily while iterating a scenario sequence when a pack file can't be read or parsed. */
class PackParseException(cause: Throwable) : Exception(cause)

interface PackContentWriter {
    /**
     * Atomically replaces the pack's content. The sequence is consumed exactly once, inside
     * the write, so scenarios stream into storage without materializing the whole pack.
     */
    suspend fun replacePack(pack: InstalledPack, scenarios: Sequence<ScenarioContent>)
    suspend fun deletePack(id: String)
}
