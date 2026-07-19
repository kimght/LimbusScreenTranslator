package com.kimght.limbusscreentranslator.data.install

import com.kimght.limbusscreentranslator.data.network.dto.LineDto
import com.kimght.limbusscreentranslator.data.network.dto.ModelCodeDto
import com.kimght.limbusscreentranslator.data.network.dto.ModelCodesDto
import com.kimght.limbusscreentranslator.data.network.dto.ScenarioDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScenarioMapperTest {

    private val table = ScenarioMapper.buildModelTable(
        ModelCodesDto(
            dataList = listOf(
                ModelCodeDto(id = "그레고르", name = "Грегор", nickName = "Грешник №13"),
                ModelCodeDto(id = "", name = "ignored", nickName = "ignored"),
            ),
        ),
    )

    @Test
    fun `buildModelTable skips blank ids`() {
        assertEquals(1, table.size)
        assertEquals("Грегор", table["그레고르"]?.name)
    }

    @Test
    fun `spacers are dropped and indices stay contiguous`() {
        val scenario = ScenarioDto(
            dataList = listOf(
                LineDto(id = "0", content = "first"),
                LineDto(id = "1", content = null),
                LineDto(id = "2", content = "  "),
                LineDto(id = "3", content = "second"),
            ),
        )
        val lines = ScenarioMapper.toDialogueLines(scenario, table)
        assertEquals(2, lines.size)
        assertEquals(0, lines[0].index)
        assertEquals("first", lines[0].text)
        assertEquals(1, lines[1].index)
        assertEquals("second", lines[1].text)
    }

    @Test
    fun `speaker is resolved at mapping time and place is carried through`() {
        val scenario = ScenarioDto(
            dataList = listOf(
                LineDto(id = "0", model = "그레고르", content = "hi", place = "Branch"),
            ),
        )
        val line = ScenarioMapper.toDialogueLines(scenario, table).single()
        assertEquals("Грегор", line.speakerName)
        assertEquals("Грешник №13", line.title)
        assertEquals("Branch", line.place)
    }

    @Test
    fun `narration has no speaker and blank place becomes null`() {
        val scenario = ScenarioDto(
            dataList = listOf(LineDto(id = "narration", content = "narration", place = " ")),
        )
        val line = ScenarioMapper.toDialogueLines(scenario, table).single()
        assertNull(line.speakerName)
        assertNull(line.title)
        assertNull(line.place)
    }
}
