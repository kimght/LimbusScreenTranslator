package com.kimght.LimbusScreenTranslator.overlay.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.kimght.LimbusScreenTranslator.domain.markup.TextSegment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DialogueMarkupTextTest {

    @Test
    fun `plain segments carry no span styles`() {
        val result = listOf(TextSegment("hello")).toAnnotatedString()
        assertEquals("hello", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `styled segments become span styles over the right ranges`() {
        val result = listOf(
            TextSegment("plain "),
            TextSegment("bold", bold = true),
            TextSegment("red", color = 0xFFFF0000),
        ).toAnnotatedString()

        assertEquals("plain boldred", result.text)
        assertEquals(2, result.spanStyles.size)

        val boldSpan = result.spanStyles[0]
        assertEquals(6, boldSpan.start)
        assertEquals(10, boldSpan.end)
        assertEquals(FontWeight.Bold, boldSpan.item.fontWeight)
        assertEquals(Color.Unspecified, boldSpan.item.color)

        val redSpan = result.spanStyles[1]
        assertEquals(10, redSpan.start)
        assertEquals(13, redSpan.end)
        assertEquals(Color(0xFFFF0000), redSpan.item.color)
    }

    @Test
    fun `italic maps to font style`() {
        val result = listOf(TextSegment("it", italic = true)).toAnnotatedString()
        assertEquals(FontStyle.Italic, result.spanStyles.single().item.fontStyle)
    }
}
