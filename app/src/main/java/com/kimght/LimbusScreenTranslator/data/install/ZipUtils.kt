package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import java.util.zip.ZipFile

object ZipUtils {

    private const val STORY_DATA_DIR = "StoryData"

    /**
     * Finds the language directory directly containing `StoryData/` among the archive's
     * entries and returns it as an entry-name prefix ("" for the root, else e.g. "MyLang/").
     * Nothing is extracted to disk; scenario files are parsed straight from the archive.
     */
    fun locateLanguageDir(archive: ZipFile, format: PackFormat): String? = when (format) {
        PackFormat.NEW -> "".takeIf { it in storyDataParentPrefixes(archive) }
        PackFormat.AUTO, PackFormat.COMPATIBLE ->
            storyDataParentPrefixes(archive)
                .minWithOrNull(compareBy({ prefix -> prefix.count { it == '/' } }, { it }))
        PackFormat.UNKNOWN -> null
    }

    private fun storyDataParentPrefixes(archive: ZipFile): Set<String> {
        val prefixes = mutableSetOf<String>()
        for (entry in archive.entries()) {
            val segments = entry.name.split('/').filter { it.isNotEmpty() }
            // A segment only proves a StoryData *directory* if something follows it in the
            // path or the entry itself is a directory.
            segments.forEachIndexed { i, segment ->
                if (segment == STORY_DATA_DIR && (i < segments.lastIndex || entry.isDirectory)) {
                    prefixes += segments.take(i).joinToString("") { "$it/" }
                }
            }
        }
        return prefixes
    }
}
