package com.kimght.LimbusScreenTranslator.overlay.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.kimght.LimbusScreenTranslator.domain.markup.TextSegment

fun List<TextSegment>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    for (segment in this@toAnnotatedString) {
        if (!segment.bold && !segment.italic && segment.color == null) {
            append(segment.text)
            continue
        }
        val style = SpanStyle(
            color = segment.color?.let { Color(it) } ?: Color.Unspecified,
            fontWeight = FontWeight.Bold.takeIf { segment.bold },
            fontStyle = FontStyle.Italic.takeIf { segment.italic },
        )
        withStyle(style) { append(segment.text) }
    }
}
