package com.kimght.LimbusScreenTranslator.data.install

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PackContentParserTest {

    private lateinit var langDir: File

    @Before
    fun setUp() {
        langDir = Files.createTempDirectory("lang").toFile()
        File(langDir, "StoryData").mkdirs()
    }

    @After
    fun tearDown() {
        langDir.deleteRecursively()
    }

    private fun writeStory(name: String, json: String) {
        File(langDir, "StoryData/$name").writeText(json, Charsets.UTF_8)
    }

    @Test
    fun `parses scenarios, drops spacers and resolves speakers from model table`() {
        File(langDir, "StoryData/ScenarioModelCodes-AutoCreated.json").writeText(
            """{"dataList":[{"id":"그레고르","name":"Грегор","nickName":"Грешник №13"}]}""",
            Charsets.UTF_8,
        )
        writeStory(
            "S001B.json",
            """
            {"dataList":[
              {"id":0,"place":"Test","content":"narration"},
              {"id":1},
              {"id":2,"model":"그레고르","content":"hello"}
            ]}
            """.trimIndent(),
        )

        val content = PackContentParser.parse(langDir)

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
        File(langDir, "StoryData/ScenarioModelCodes-AutoCreated.json").writeText(
            """{"dataList":[]}""",
            Charsets.UTF_8,
        )
        writeStory("S001B.json", """{"dataList":[{"id":0,"content":"x"}]}""")

        val codes = PackContentParser.parse(langDir).map { it.code }

        assertEquals(listOf("S001B"), codes)
    }

    @Test
    fun `parses UTF-8 with BOM scenario files`() {
        val bom = "﻿"
        writeStory("S002A.json", bom + """{"dataList":[{"id":0,"content":"bom line"}]}""")

        val scenario = PackContentParser.parse(langDir).single()

        assertEquals("S002A", scenario.code)
        assertEquals("bom line", scenario.lines.single().text)
    }

    @Test
    fun `missing StoryData yields no scenarios`() {
        File(langDir, "StoryData").deleteRecursively()
        assertTrue(PackContentParser.parse(langDir).isEmpty())
    }
}
