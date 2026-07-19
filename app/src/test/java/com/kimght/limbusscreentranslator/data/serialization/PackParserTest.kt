package com.kimght.limbusscreentranslator.data.serialization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackParserTest {

    @Test
    fun `manifest parses snake_case fields and ignores unknown ones`() {
        val raw = """
            {
              "format_version": 1,
              "extra_metadata": { "anything": true },
              "chapters_url": "https://example.com/chapters.json",
              "localizations": [
                {
                  "id": "ru-CresCorp",
                  "version": "v0.5.0.3",
                  "name": "CresCorp",
                  "flag": "RU",
                  "icon": "data:image/webp;base64,AAAA",
                  "description": "# changelog",
                  "authors": ["A", "B"],
                  "url": "https://example.com/p.zip",
                  "size": 12345678,
                  "fonts": [{"url":"u","hash":"h","name":"F.ttf"}],
                  "format": "auto",
                  "future_field": 99
                }
              ]
            }
        """.trimIndent()

        val m = PackParser.parseManifest(raw)
        assertEquals(1, m.formatVersion)
        assertEquals("https://example.com/chapters.json", m.chaptersUrl)
        assertEquals(1, m.localizations.size)
        val loc = m.localizations.first()
        assertEquals("ru-CresCorp", loc.id)
        assertEquals("v0.5.0.3", loc.version)
        assertEquals(12345678L, loc.size)
        assertEquals(listOf("A", "B"), loc.authors)
        assertEquals("auto", loc.format)
        assertEquals(1, loc.fonts.size)
    }

    @Test
    fun `parser strips a leading UTF-8 BOM`() {
        val bom = "﻿"
        val raw = bom + """{"format_version":1,"localizations":[],"chapters_url":"x"}"""
        val m = PackParser.parseManifest(raw)
        assertEquals(1, m.formatVersion)
        assertEquals("x", m.chaptersUrl)
    }

    @Test
    fun `chapters parse with ordered episode codes`() {
        val raw = """
            {"chapters":[
              {"name":"Canto I","subtitle":"...","episodes":["S001B","S001A","S002B"]}
            ]}
        """.trimIndent()
        val c = PackParser.parseChapters(raw)
        assertEquals(1, c.chapters.size)
        assertEquals(listOf("S001B", "S001A", "S002B"), c.chapters.first().episodes)
    }

    @Test
    fun `scenario parses lines including spacers and optional fields`() {
        val raw = """
            {"dataList":[
              {"id":0,"place":"Place","content":"narration"},
              {"id":3,"model":"그레고르","content":"line"},
              {"id":7}
            ]}
        """.trimIndent()
        val s = PackParser.parseScenario(raw)
        assertEquals(3, s.dataList.size)
        assertEquals("narration", s.dataList[0].content)
        assertEquals("그레고르", s.dataList[1].model)
        assertNull(s.dataList[2].content) // spacer entry preserved at DTO level
    }

    @Test
    fun `scenario parses lines whose id is a non-numeric string`() {
        val raw = """
            {"dataList":[
              {"id":"project_gs_button_label_open_l","content":"a string-keyed line"},
              {"id":0,"content":"a numeric-keyed line"}
            ]}
        """.trimIndent()
        val s = PackParser.parseScenario(raw)
        assertEquals(2, s.dataList.size)
        assertEquals("a string-keyed line", s.dataList[0].content)
        assertEquals("a numeric-keyed line", s.dataList[1].content)
    }

    @Test
    fun `model codes parse with name and nickName`() {
        val raw = """
            {"dataList":[
              {"id":"그레고르","name":"Грегор","nickName":"Грешник №13"},
              {"id":"유리","name":"Юри","nickName":""}
            ]}
        """.trimIndent()
        val codes = PackParser.parseModelCodes(raw)
        assertEquals(2, codes.dataList.size)
        assertEquals("Грегор", codes.dataList[0].name)
        assertEquals("", codes.dataList[1].nickName)
    }
}
