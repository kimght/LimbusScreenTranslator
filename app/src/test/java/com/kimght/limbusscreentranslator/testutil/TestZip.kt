package com.kimght.limbusscreentranslator.testutil

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object TestZip {

    fun bytes(entries: List<Pair<String, String>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    fun write(file: File, entries: List<Pair<String, String>>) {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes(entries))
    }
}
