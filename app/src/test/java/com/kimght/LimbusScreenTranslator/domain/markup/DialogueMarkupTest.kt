package com.kimght.LimbusScreenTranslator.domain.markup

import org.junit.Assert.assertEquals
import org.junit.Test

class DialogueMarkupTest {

    @Test
    fun `plain text passes through as a single segment`() {
        assertEquals(listOf(TextSegment("Hello, world.")), DialogueMarkup.parse("Hello, world."))
    }

    @Test
    fun `empty input yields no segments`() {
        assertEquals(emptyList<TextSegment>(), DialogueMarkup.parse(""))
    }

    @Test
    fun `bold and italic tags produce styled segments`() {
        assertEquals(
            listOf(
                TextSegment("a "),
                TextSegment("b", bold = true),
                TextSegment(" c "),
                TextSegment("d", italic = true),
            ),
            DialogueMarkup.parse("a <b>b</b> c <i>d</i>"),
        )
    }

    @Test
    fun `overlapping bold and italic close in any order`() {
        assertEquals(
            listOf(
                TextSegment("x", bold = true, italic = true),
                TextSegment("y", italic = true),
            ),
            DialogueMarkup.parse("<b><i>x</b>y</i>"),
        )
    }

    @Test
    fun `unclosed tag persists to end of line`() {
        assertEquals(
            listOf(TextSegment("a "), TextSegment("bold to the end", bold = true)),
            DialogueMarkup.parse("a <b>bold to the end"),
        )
    }

    @Test
    fun `stray closing tag is stripped and ignored`() {
        assertEquals(listOf(TextSegment("ab")), DialogueMarkup.parse("a</b>b"))
    }

    @Test
    fun `nested bold needs matching closers`() {
        assertEquals(
            listOf(TextSegment("ab", bold = true), TextSegment("c")),
            DialogueMarkup.parse("<b>a<b>b</b></b>c"),
        )
    }

    @Test
    fun `hex colors in RRGGBB form`() {
        assertEquals(
            listOf(TextSegment("red", color = 0xFFFF0000)),
            DialogueMarkup.parse("<color=#FF0000>red</color>"),
        )
    }

    @Test
    fun `hex colors in short RGB form expand`() {
        assertEquals(
            listOf(TextSegment("red", color = 0xFFFF0000)),
            DialogueMarkup.parse("<color=#F00>red</color>"),
        )
    }

    @Test
    fun `hex colors in RRGGBBAA form carry alpha`() {
        assertEquals(
            listOf(TextSegment("faint", color = 0x80FF0000)),
            DialogueMarkup.parse("<color=#FF000080>faint</color>"),
        )
    }

    @Test
    fun `named colors are supported`() {
        assertEquals(
            listOf(TextSegment("r", color = 0xFFFF0000), TextSegment("g", color = 0xFF008000)),
            DialogueMarkup.parse("<color=red>r</color><color=green>g</color>"),
        )
    }

    @Test
    fun `closing color restores the previous color`() {
        assertEquals(
            listOf(
                TextSegment("a", color = 0xFFFF0000),
                TextSegment("b", color = 0xFF0000FF),
                TextSegment("c", color = 0xFFFF0000),
                TextSegment("d"),
            ),
            DialogueMarkup.parse("<color=red>a<color=blue>b</color>c</color>d"),
        )
    }

    @Test
    fun `invalid color value strips the tag without coloring`() {
        assertEquals(
            listOf(TextSegment("text")),
            DialogueMarkup.parse("<color=notacolor>text</color>"),
        )
    }

    @Test
    fun `unclosed color persists to end of line`() {
        assertEquals(
            listOf(TextSegment("a "), TextSegment("blue rest", color = 0xFF0000FF)),
            DialogueMarkup.parse("a <color=#0000FF>blue rest"),
        )
    }

    @Test
    fun `ruby with quoted annotation renders text then annotation`() {
        assertEquals(
            listOf(TextSegment("YYY (XXX) end")),
            DialogueMarkup.parse("<ruby=\"XXX\">YYY</ruby> end"),
        )
    }

    @Test
    fun `ruby with unquoted annotation renders text then annotation`() {
        assertEquals(
            listOf(TextSegment("YYY (XXX)")),
            DialogueMarkup.parse("<ruby=XXX>YYY</ruby>"),
        )
    }

    @Test
    fun `unclosed ruby appends annotation at end of line`() {
        assertEquals(
            listOf(TextSegment("YYY rest (XXX)")),
            DialogueMarkup.parse("<ruby=XXX>YYY rest"),
        )
    }

    @Test
    fun `ruby content keeps active styles and annotation styles at the closer`() {
        assertEquals(
            listOf(TextSegment("YYY (XXX)", bold = true)),
            DialogueMarkup.parse("<b><ruby=XXX>YYY</ruby></b>"),
        )
    }

    @Test
    fun `size tags are stripped keeping inner text`() {
        assertEquals(
            listOf(TextSegment("big and after")),
            DialogueMarkup.parse("<size=120%>big</size> and after"),
        )
    }

    @Test
    fun `known TMP tags are stripped keeping inner text`() {
        assertEquals(
            listOf(TextSegment("under ")),
            DialogueMarkup.parse("<u>under</u> <sprite=0>"),
        )
    }

    @Test
    fun `br becomes a newline`() {
        assertEquals(listOf(TextSegment("a\nb")), DialogueMarkup.parse("a<br>b"))
    }

    @Test
    fun `unknown tags and lone angle brackets stay literal`() {
        assertEquals(
            listOf(TextSegment("<3 x < y <weird>tag")),
            DialogueMarkup.parse("<3 x < y <weird>tag"),
        )
    }

    @Test
    fun `angle bracket without closing stays literal`() {
        assertEquals(listOf(TextSegment("a <b")), DialogueMarkup.parse("a <b"))
    }

    @Test
    fun `adjacent segments with identical style merge`() {
        assertEquals(
            listOf(TextSegment("ab", bold = true)),
            DialogueMarkup.parse("<b>a</i>b</b>"),
        )
    }
}
