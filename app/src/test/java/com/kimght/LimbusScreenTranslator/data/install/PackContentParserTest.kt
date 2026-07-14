package com.kimght.LimbusScreenTranslator.data.install

import com.kimght.LimbusScreenTranslator.testutil.TestZip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class PackContentParserTest {

    private lateinit var tmp: File
    private val openArchives = mutableListOf<ZipFile>()

    @Before
    fun setUp() {
        tmp = Files.createTempDirectory("parser").toFile()
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
    fun `parses scenarios, drops spacers and resolves speakers from model table`() {
        val zip = archive(
            "MyLang/StoryData/ScenarioModelCodes-AutoCreated.json" to
                """{"dataList":[{"id":"그레고르","name":"Грегор","nickName":"Грешник №13"}]}""",
            "MyLang/StoryData/S001B.json" to
                """
                {"dataList":[
                  {"id":0,"place":"Test","content":"narration"},
                  {"id":1},
                  {"id":2,"model":"그레고르","content":"hello"}
                ]}
                """.trimIndent(),
        )

        val content = PackContentParser.parse(zip, "MyLang/").toList()

        assertEquals(1, content.size)
        val scenario = content.single()
        assertEquals("S001B", scenario.code)
        assertEquals(2, scenario.lines.size)
        assertNull(scenario.lines[0].speakerName)
        assertEquals("Test", scenario.lines[0].place)
        assertEquals("Грегор", scenario.lines[1].speakerName)
        assertEquals("Грешник №13", scenario.lines[1].title)
    }

    @Test
    fun `model codes file is excluded from scenario list`() {
        val zip = archive(
            "StoryData/ScenarioModelCodes-AutoCreated.json" to """{"dataList":[]}""",
            "StoryData/S001B.json" to """{"dataList":[{"id":0,"content":"x"}]}""",
        )

        val codes = PackContentParser.parse(zip, "").map { it.code }.toList()

        assertEquals(listOf("S001B"), codes)
    }

    @Test
    fun `model codes file at the language dir root is used`() {
        val zip = archive(
            "MyLang/ScenarioModelCodes-AutoCreated.json" to
                """{"dataList":[{"id":"이상","name":"Isang","nickName":"N"}]}""",
            "MyLang/StoryData/S001B.json" to
                """{"dataList":[{"id":0,"model":"이상","content":"x"}]}""",
        )

        val scenario = PackContentParser.parse(zip, "MyLang/").single()

        assertEquals("Isang", scenario.lines.single().speakerName)
    }

    @Test
    fun `parses UTF-8 with BOM scenario files`() {
        val bom = "﻿"
        val zip = archive(
            "StoryData/S002A.json" to bom + """{"dataList":[{"id":0,"content":"bom line"}]}""",
        )

        val scenario = PackContentParser.parse(zip, "").single()

        assertEquals("S002A", scenario.code)
        assertEquals("bom line", scenario.lines.single().text)
    }

    @Test
    fun `files nested below StoryData are ignored`() {
        val zip = archive(
            "StoryData/S001B.json" to """{"dataList":[{"id":0,"content":"x"}]}""",
            "StoryData/nested/S999Z.json" to """{"dataList":[{"id":0,"content":"y"}]}""",
        )

        assertEquals(listOf("S001B"), PackContentParser.parse(zip, "").map { it.code }.toList())
    }

    @Test
    fun `missing StoryData yields no scenarios`() {
        val zip = archive("readme.txt" to "nothing here")

        assertTrue(PackContentParser.parse(zip, "").toList().isEmpty())
    }

    @Test
    fun `a malformed scenario surfaces as PackParseException on iteration`() {
        val zip = archive("StoryData/S001B.json" to "{ not json")

        val scenarios = PackContentParser.parse(zip, "")

        assertThrows(PackParseException::class.java) { scenarios.toList() }
    }

    @Test
    fun `scenarios are parsed lazily, one file at a time`() {
        val zip = archive(
            "StoryData/A.json" to """{"dataList":[{"id":0,"content":"first"}]}""",
            "StoryData/B.json" to "{ not json",
        )

        // Files sort A before B, so the first element parses fine even though B is broken.
        val first = PackContentParser.parse(zip, "").first()

        assertEquals("A", first.code)
        assertEquals("first", first.lines.single().text)
    }
}
