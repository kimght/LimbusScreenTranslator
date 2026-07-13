package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.data.serialization.PackParser
import com.kimght.LimbusScreenTranslator.domain.ModelInfo
import java.io.File

object PackContentParser {

    private const val STORY_DATA_DIR = "StoryData"
    private const val MODEL_CODES_FILE = "ScenarioModelCodes-AutoCreated.json"

    fun parse(languageDir: File): List<ScenarioContent> {
        val storyData = File(languageDir, STORY_DATA_DIR)
        if (!storyData.isDirectory) return emptyList()

        val modelTable = readModelTable(languageDir, storyData)

        return storyData.listFiles { f ->
            f.isFile && f.extension.equals(
                "json",
                ignoreCase = true
            )
        }
            .orEmpty()
            .filter { it.name != MODEL_CODES_FILE }
            .sortedBy { it.name }
            .map { file ->
                val scenario = PackParser.parseScenario(file.readText(Charsets.UTF_8))
                ScenarioContent(
                    code = file.nameWithoutExtension,
                    lines = ScenarioMapper.toDialogueLines(scenario, modelTable),
                )
            }
    }

    private fun readModelTable(languageDir: File, storyData: File): Map<String, ModelInfo> {
        val candidate = listOf(
            File(storyData, MODEL_CODES_FILE),
            File(languageDir, MODEL_CODES_FILE),
        ).firstOrNull { it.isFile } ?: return emptyMap()

        val codes = PackParser.parseModelCodes(candidate.readText(Charsets.UTF_8))
        return ScenarioMapper.buildModelTable(codes)
    }
}
