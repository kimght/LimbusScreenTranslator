package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import com.kimght.LimbusScreenTranslator.testutil.TestZip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class ZipUtilsTest {

    private lateinit var tmp: File
    private val openArchives = mutableListOf<ZipFile>()

    @Before
    fun setUp() {
        tmp = Files.createTempDirectory("ziputils").toFile()
    }

    @After
    fun tearDown() {
        openArchives.forEach { it.close() }
        tmp.deleteRecursively()
    }

    private fun archive(vararg entries: Pair<String, String>): ZipFile {
        val file = File(tmp, "pack-${openArchives.size}.zip")
        TestZip.write(file, entries.toList())
        return ZipFile(file).also { openArchives += it }
    }

    @Test
    fun `locate AUTO finds the nested folder containing StoryData`() {
        val zip = archive("wrapper/MyLang/StoryData/S001B.json" to "{}")

        assertEquals("wrapper/MyLang/", ZipUtils.locateLanguageDir(zip, PackFormat.AUTO))
    }

    @Test
    fun `locate AUTO prefers the shallowest StoryData parent`() {
        val zip = archive(
            "deep/nested/OtherLang/StoryData/S001B.json" to "{}",
            "TopLang/StoryData/S001B.json" to "{}",
        )

        assertEquals("TopLang/", ZipUtils.locateLanguageDir(zip, PackFormat.AUTO))
    }

    @Test
    fun `locate AUTO resolves StoryData at the archive root`() {
        val zip = archive("StoryData/S001B.json" to "{}")

        assertEquals("", ZipUtils.locateLanguageDir(zip, PackFormat.AUTO))
    }

    @Test
    fun `locate NEW returns the root only when it has StoryData`() {
        val withStoryData = archive("StoryData/S001B.json" to "{}")
        val nestedOnly = archive("MyLang/StoryData/S001B.json" to "{}")

        assertEquals("", ZipUtils.locateLanguageDir(withStoryData, PackFormat.NEW))
        assertNull(ZipUtils.locateLanguageDir(nestedOnly, PackFormat.NEW))
    }

    @Test
    fun `locate returns null when no StoryData exists`() {
        val zip = archive("MyLang/readme.txt" to "no story data")

        assertNull(ZipUtils.locateLanguageDir(zip, PackFormat.AUTO))
    }

    @Test
    fun `a plain file named StoryData is not a language dir`() {
        val zip = archive("MyLang/StoryData" to "not a directory")

        assertNull(ZipUtils.locateLanguageDir(zip, PackFormat.AUTO))
    }

    @Test
    fun `locate UNKNOWN never resolves`() {
        val zip = archive("StoryData/S001B.json" to "{}")

        assertNull(ZipUtils.locateLanguageDir(zip, PackFormat.UNKNOWN))
    }
}
