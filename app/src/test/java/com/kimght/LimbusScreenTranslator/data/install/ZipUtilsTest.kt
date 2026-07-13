package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.domain.model.PackFormat
import com.kimght.LimbusScreenTranslator.testutil.TestZip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ZipUtilsTest {

    private lateinit var tmp: File

    @Before
    fun setUp() {
        tmp = Files.createTempDirectory("ziputils").toFile()
    }

    @After
    fun tearDown() {
        tmp.deleteRecursively()
    }

    @Test
    fun `extract writes entries and skips path-traversal entries`() {
        val zip = File(tmp, "pack.zip")
        TestZip.write(
            zip,
            listOf(
                "MyLang/StoryData/S001B.json" to "{}",
                "../../evil.txt" to "pwned",
                "MyLang/StoryData/../../../escape.txt" to "pwned",
            ),
        )
        val dest = File(tmp, "out")

        ZipUtils.extract(zip, dest)

        assertTrue(File(dest, "MyLang/StoryData/S001B.json").isFile)
        assertFalse(File(tmp, "evil.txt").exists())
        assertFalse(File(tmp, "escape.txt").exists())
    }

    @Test
    fun `locate AUTO finds the nested folder containing StoryData`() {
        val dest = File(tmp, "out").apply { mkdirs() }
        File(dest, "wrapper/MyLang/StoryData").mkdirs()

        val langDir = ZipUtils.locateLanguageDir(dest, PackFormat.AUTO)

        assertEquals("MyLang", langDir?.name)
    }

    @Test
    fun `locate NEW returns root only when it has StoryData`() {
        val withStoryData = File(tmp, "root1").apply { File(this, "StoryData").mkdirs() }
        val without = File(tmp, "root2").apply { mkdirs() }

        assertEquals(withStoryData, ZipUtils.locateLanguageDir(withStoryData, PackFormat.NEW))
        assertNull(ZipUtils.locateLanguageDir(without, PackFormat.NEW))
    }

    @Test
    fun `locate UNKNOWN never resolves`() {
        val dir = File(tmp, "root").apply { File(this, "StoryData").mkdirs() }
        assertNull(ZipUtils.locateLanguageDir(dir, PackFormat.UNKNOWN))
    }
}
