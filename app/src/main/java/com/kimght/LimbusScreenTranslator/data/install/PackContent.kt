package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.domain.model.DialogueLine
import com.kimght.LimbusScreenTranslator.domain.model.InstalledPack

data class ScenarioContent(
    val code: String,
    val lines: List<DialogueLine>,
)

interface PackContentWriter {
    suspend fun replacePack(pack: InstalledPack, scenarios: List<ScenarioContent>)
    suspend fun deletePack(id: String)
}
