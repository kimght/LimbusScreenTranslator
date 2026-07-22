package com.kimght.limbusscreentranslator.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    @Test
    fun `parses the four block types and drops blank lines`() {
        val md = "# v2.1.0\n\n## Changes\n- First\n- Second\nA closing paragraph."

        val blocks = parseMarkdown(md)

        assertEquals(
            listOf(
                MarkdownBlock.H1("v2.1.0"),
                MarkdownBlock.H2("Changes"),
                MarkdownBlock.ListItem("First"),
                MarkdownBlock.ListItem("Second"),
                MarkdownBlock.Paragraph("A closing paragraph."),
            ),
            blocks,
        )
    }

    @Test
    fun `h2 is detected before h1 so double-hash is not a heading-one`() {
        assertEquals(listOf(MarkdownBlock.H2("Sub")), parseMarkdown("## Sub"))
    }

    @Test
    fun `empty description yields no blocks`() {
        assertEquals(emptyList<MarkdownBlock>(), parseMarkdown(""))
    }

    @Test
    fun `triple-hash is a heading-three detected before h2 and h1`() {
        assertEquals(listOf(MarkdownBlock.H3("Detail")), parseMarkdown("### Detail"))
    }

    @Test
    fun `four hashes create a heading-four block`() {
        assertEquals(listOf(MarkdownBlock.H4("Fine detail")), parseMarkdown("#### Fine detail"))
    }

    @Test
    fun `five hashes remain paragraph text`() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph("##### Too deep")),
            parseMarkdown("##### Too deep"),
        )
    }

    @Test
    fun `a line of three dashes is a horizontal rule`() {
        assertEquals(listOf(MarkdownBlock.Rule), parseMarkdown("---"))
    }

    @Test
    fun `extra dashes and surrounding whitespace still parse as a rule`() {
        assertEquals(listOf(MarkdownBlock.Rule), parseMarkdown("  -----  "))
    }

    @Test
    fun `a list item is not mistaken for a rule`() {
        assertEquals(listOf(MarkdownBlock.ListItem("item")), parseMarkdown("- item"))
    }

    @Test
    fun `plain text is a single unstyled inline span`() {
        assertEquals(listOf(InlineSpan("hello world")), parseInline("hello world"))
    }

    @Test
    fun `double-star wraps a bold span and strips the markers`() {
        assertEquals(
            listOf(
                InlineSpan("a "),
                InlineSpan("bold", bold = true),
                InlineSpan(" b"),
            ),
            parseInline("a **bold** b"),
        )
    }

    @Test
    fun `single-star wraps an italic span and strips the markers`() {
        assertEquals(
            listOf(
                InlineSpan("a "),
                InlineSpan("soft", italic = true),
                InlineSpan(" b"),
            ),
            parseInline("a *soft* b"),
        )
    }

    @Test
    fun `an unmatched star is kept as a literal`() {
        assertEquals(listOf(InlineSpan("2 * 3 = 6")), parseInline("2 * 3 = 6"))
    }

    @Test
    fun `single underscore wraps an italic span`() {
        assertEquals(
            listOf(InlineSpan("soft", italic = true)),
            parseInline("_soft_"),
        )
    }

    @Test
    fun `double underscore wraps a bold span`() {
        assertEquals(
            listOf(InlineSpan("strong", bold = true)),
            parseInline("__strong__"),
        )
    }

    @Test
    fun `unmatched underscore is kept as a literal`() {
        assertEquals(listOf(InlineSpan("file_name")), parseInline("file_name"))
    }

    @Test
    fun `italic underscore can nest inside bold underscores`() {
        assertEquals(
            listOf(
                InlineSpan("bold and ", bold = true),
                InlineSpan("italic", bold = true, italic = true),
                InlineSpan(" text", bold = true),
            ),
            parseInline("__bold and _italic_ text__"),
        )
    }

    @Test
    fun `underscore italic can nest inside asterisk bold`() {
        assertEquals(
            listOf(
                InlineSpan("bold ", bold = true),
                InlineSpan("italic", bold = true, italic = true),
            ),
            parseInline("**bold _italic_**"),
        )
    }

    @Test
    fun `asterisk bold can nest inside asterisk italic`() {
        assertEquals(
            listOf(
                InlineSpan("italic ", italic = true),
                InlineSpan("bold", bold = true, italic = true),
                InlineSpan(" text", italic = true),
            ),
            parseInline("*italic **bold** text*"),
        )
    }

    @Test
    fun `underscore bold can nest inside underscore italic`() {
        assertEquals(
            listOf(
                InlineSpan("italic ", italic = true),
                InlineSpan("bold", bold = true, italic = true),
                InlineSpan(" text", italic = true),
            ),
            parseInline("_italic __bold__ text_"),
        )
    }

    @Test
    fun `unmatched single opener stays literal before double emphasis`() {
        assertEquals(
            listOf(
                InlineSpan("*literal "),
                InlineSpan("bold", bold = true),
            ),
            parseInline("*literal **bold**"),
        )
    }

    @Test
    fun `unmatched double asterisk stays literal before single emphasis`() {
        assertEquals(
            listOf(
                InlineSpan("**literal "),
                InlineSpan("italic", italic = true),
            ),
            parseInline("**literal *italic*"),
        )
    }

    @Test
    fun `unmatched double underscore stays literal before single emphasis`() {
        assertEquals(
            listOf(
                InlineSpan("__literal "),
                InlineSpan("italic", italic = true),
            ),
            parseInline("__literal _italic_"),
        )
    }
}
