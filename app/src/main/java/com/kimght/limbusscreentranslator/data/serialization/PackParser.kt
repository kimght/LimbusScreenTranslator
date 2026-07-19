package com.kimght.limbusscreentranslator.data.serialization

import com.kimght.limbusscreentranslator.data.network.dto.ChaptersDto
import com.kimght.limbusscreentranslator.data.network.dto.ManifestDto
import com.kimght.limbusscreentranslator.data.network.dto.ModelCodesDto
import com.kimght.limbusscreentranslator.data.network.dto.ScenarioDto
import kotlinx.serialization.json.Json

object PackParser {

    private const val BOM = '\uFEFF'

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private fun String.stripBom(): String = if (startsWith(BOM)) substring(1) else this

    fun parseManifest(raw: String): ManifestDto = json.decodeFromString(raw.stripBom())

    fun parseChapters(raw: String): ChaptersDto = json.decodeFromString(raw.stripBom())

    fun parseScenario(raw: String): ScenarioDto = json.decodeFromString(raw.stripBom())

    fun parseModelCodes(raw: String): ModelCodesDto = json.decodeFromString(raw.stripBom())
}
