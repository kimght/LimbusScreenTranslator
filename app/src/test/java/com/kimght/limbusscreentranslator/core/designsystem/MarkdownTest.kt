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
}
