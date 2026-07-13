package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import java.io.File
import java.util.zip.ZipFile

object ZipUtils {

    private const val STORY_DATA_DIR = "StoryData"

    fun extract(zip: File, destDir: File) {
        if (!destDir.exists()) destDir.mkdirs()
        val destRoot = destDir.canonicalFile
        ZipFile(zip).use { archive ->
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val target = File(destRoot, entry.name).canonicalFile
                if (!target.path.startsWith(destRoot.path + File.separator) && target.path != destRoot.path) {
                    continue
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    archive.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    fun locateLanguageDir(root: File, format: PackFormat): File? = when (format) {
        PackFormat.NEW -> root.takeIf { hasStoryData(it) }
        PackFormat.AUTO, PackFormat.COMPATIBLE -> findStoryDataParent(root)
        PackFormat.UNKNOWN -> null
    }

    private fun hasStoryData(dir: File): Boolean = File(dir, STORY_DATA_DIR).isDirectory

    private fun findStoryDataParent(root: File): File? {
        if (!root.isDirectory) return null
        val queue = ArrayDeque<File>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            if (hasStoryData(dir)) return dir
            dir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name }
                ?.forEach { queue.add(it) }
        }
        return null
    }
}
