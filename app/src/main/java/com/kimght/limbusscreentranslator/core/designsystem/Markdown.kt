package com.kimght.limbusscreentranslator.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimght.limbusscreentranslator.ui.theme.Limbus200
import com.kimght.limbusscreentranslator.ui.theme.Limbus300
import com.kimght.limbusscreentranslator.ui.theme.Limbus500
import com.kimght.limbusscreentranslator.ui.theme.Limbus600

sealed interface MarkdownBlock {
    data class H1(val text: String) : MarkdownBlock
    data class H2(val text: String) : MarkdownBlock
    data class H3(val text: String) : MarkdownBlock
    data class H4(val text: String) : MarkdownBlock
    data class ListItem(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

private val RULE = Regex("-{3,}")

fun parseMarkdown(md: String): List<MarkdownBlock> =
    md.split("\n")
        .filter { it.isNotBlank() }
        .map { line ->
            when {
                RULE.matches(line.trim()) -> MarkdownBlock.Rule
                line.startsWith("#### ") -> MarkdownBlock.H4(line.substring(5))
                line.startsWith("### ") -> MarkdownBlock.H3(line.substring(4))
                line.startsWith("## ") -> MarkdownBlock.H2(line.substring(3))
                line.startsWith("# ") -> MarkdownBlock.H1(line.substring(2))
                line.startsWith("- ") -> MarkdownBlock.ListItem(line.substring(2))
                else -> MarkdownBlock.Paragraph(line)
            }
        }

fun parseInline(text: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()

    fun append(text: String, bold: Boolean, italic: Boolean) {
        if (text.isEmpty()) return
        val previous = spans.lastOrNull()
        if (previous != null && previous.bold == bold && previous.italic == italic) {
            spans[spans.lastIndex] = previous.copy(text = previous.text + text)
        } else {
            spans += InlineSpan(text, bold, italic)
        }
    }

    fun findClosing(marker: String, start: Int, end: Int): Int? {
        var index = start
        while (index < end) {
            if (text[index] != marker[0]) {
                index++
                continue
            }

            val length = if (index + 1 < text.length && text[index + 1] == marker[0]) 2 else 1
            if (length == marker.length) return index
            index += length
        }
        return null
    }

    fun parseRange(start: Int, end: Int, bold: Boolean, italic: Boolean) {
        var plainStart = start
        var index = start

        fun flushPlain(until: Int) {
            append(text.substring(plainStart, until), bold, italic)
        }

        while (index < end) {
            val marker = when {
                text.startsWith("**", index) -> "**"
                text.startsWith("__", index) -> "__"
                text[index] == '*' -> "*"
                text[index] == '_' -> "_"
                else -> null
            }
            val closing = marker?.let { findClosing(it, index + it.length, end) }

            if (marker != null && closing != null) {
                flushPlain(index)
                val nextBold = bold || marker.length == 2
                val nextItalic = italic || marker.length == 1
                parseRange(index + marker.length, closing, nextBold, nextItalic)
                index = closing + marker.length
                plainStart = index
            } else {
                index += marker?.length ?: 1
            }
        }
        flushPlain(end)
    }

    parseRange(0, text.length, bold = false, italic = false)
    return spans
}

@Composable
fun MarkdownChangelog(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.H1 -> Text(
                    text = inline(block.text.uppercase()),
                    color = Limbus300,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                is MarkdownBlock.H2 -> Text(
                    text = inline(block.text),
                    color = Limbus500,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 13.dp, bottom = 6.dp),
                )

                is MarkdownBlock.H3 -> Text(
                    text = inline(block.text),
                    color = Limbus500,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(top = 9.dp, bottom = 4.dp),
                )

                is MarkdownBlock.H4 -> Text(
                    text = inline(block.text),
                    color = Limbus500,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp,
                    modifier = Modifier.padding(top = 7.dp, bottom = 3.dp),
                )

                is MarkdownBlock.ListItem -> Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(text = "·", color = Limbus600, fontSize = 16.sp, lineHeight = 20.sp)
                    Text(
                        text = inline(block.text),
                        color = Limbus200,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    )
                }

                is MarkdownBlock.Paragraph -> Text(
                    text = inline(block.text),
                    color = MarkdownBodyColor,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                MarkdownBlock.Rule -> HorizontalDivider(
                    color = Limbus600,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    parseInline(text).forEach { span ->
        withStyle(
            SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
            ),
        ) {
            append(span.text)
        }
    }
}

private val MarkdownBodyColor = Limbus200
