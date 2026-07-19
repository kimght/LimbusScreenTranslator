package com.kimght.limbusscreentranslator.data.install

import com.kimght.limbusscreentranslator.data.serialization.PackParser
import com.kimght.limbusscreentranslator.domain.ModelInfo
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object PackContentParser {

    private const val STORY_DATA_DIR = "StoryData"
    private const val MODEL_CODES_FILE = "ScenarioModelCodes-AutoCreated.json"

    /**
     * Lazily parses the scenarios under `<languageDirPrefix>StoryData/`, one file per element,
     * so a whole pack is never materialized in memory. The sequence must be consumed while
     * [archive] is still open; read or parse failures surface as [PackParseException].
     */
    fun parse(archive: ZipFile, languageDirPrefix: String): Sequence<ScenarioContent> = sequence {
        val storyPrefix = "$languageDirPrefix$STORY_DATA_DIR/"
        val scenarioEntries = archive.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith(storyPrefix) }
            .map { it to it.name.removePrefix(storyPrefix) }
            .filter { (_, fileName) ->
                '/' !in fileName &&
                    fileName != MODEL_CODES_FILE &&
                    fileName.substringAfterLast('.', "").equals("json", ignoreCase = true)
            }
            .sortedBy { (_, fileName) -> fileName }
            .toList()

        val modelTable = readModelTable(archive, languageDirPrefix, storyPrefix)

        for ((entry, fileName) in scenarioEntries) {
            val scenario = parsing { PackParser.parseScenario(archive.readText(entry)) }
            yield(
                ScenarioContent(
                    code = fileName.substringBeforeLast('.'),
                    lines = parsing { ScenarioMapper.toDialogueLines(scenario, modelTable) },
                ),
            )
        }
    }

    private fun readModelTable(
        archive: ZipFile,
        languageDirPrefix: String,
        storyPrefix: String,
    ): Map<String, ModelInfo> {
        val candidate = listOf(
            "$storyPrefix$MODEL_CODES_FILE",
            "$languageDirPrefix$MODEL_CODES_FILE",
        ).firstNotNullOfOrNull { archive.getEntry(it) } ?: return emptyMap()

        val codes = parsing { PackParser.parseModelCodes(archive.readText(candidate)) }
        return ScenarioMapper.buildModelTable(codes)
    }

    private fun ZipFile.readText(entry: ZipEntry): String = parsing {
        getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private inline fun <T> parsing(block: () -> T): T = try {
        block()
    } catch (e: PackParseException) {
        throw e
    } catch (e: Exception) {
        throw PackParseException(e)
    }
}
