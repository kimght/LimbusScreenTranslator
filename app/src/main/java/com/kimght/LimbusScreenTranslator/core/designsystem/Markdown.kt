package com.kimght.LimbusScreenTranslator.core.designsystem

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
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus200
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus300
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus500
import com.kimght.LimbusScreenTranslator.ui.theme.Limbus600

sealed interface MarkdownBlock {
    data class H1(val text: String) : MarkdownBlock
    data class H2(val text: String) : MarkdownBlock
    data class H3(val text: String) : MarkdownBlock
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
                line.startsWith("### ") -> MarkdownBlock.H3(line.substring(4))
                line.startsWith("## ") -> MarkdownBlock.H2(line.substring(3))
                line.startsWith("# ") -> MarkdownBlock.H1(line.substring(2))
                line.startsWith("- ") -> MarkdownBlock.ListItem(line.substring(2))
                else -> MarkdownBlock.Paragraph(line)
            }
        }

fun parseInline(text: String): List<InlineSpan> {
    val spans = mutableListOf<InlineSpan>()
    val buf = StringBuilder()
    var bold = false
    var italic = false

    fun flush() {
        if (buf.isNotEmpty()) {
            spans.add(InlineSpan(buf.toString(), bold, italic))
            buf.clear()
        }
    }

    var i = 0
    while (i < text.length) {
        if (text[i] == '*') {
            val double = i + 1 < text.length && text[i + 1] == '*'
            val marker = if (double) "**" else "*"
            val open = if (double) bold else italic
            when {
                open -> {
                    flush()
                    if (double) bold = false else italic = false
                    i += marker.length
                    continue
                }

                text.indexOf(marker, i + marker.length) != -1 -> {
                    flush()
                    if (double) bold = true else italic = true
                    i += marker.length
                    continue
                }
            }
        }
        buf.append(text[i])
        i++
    }
    flush()
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

                is MarkdownBlock.ListItem -> Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(text = "·", color = Limbus600, fontSize = 13.sp, lineHeight = 20.sp)
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
