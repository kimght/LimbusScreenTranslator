package com.kimght.LimbusScreenTranslator.domain.markup

data class TextSegment(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val color: Long? = null,
)

object DialogueMarkup {

    private data class Tag(val name: String, val value: String?, val closing: Boolean)

    private val NAME_PATTERN = Regex("[a-z][a-z0-9-]*")

    private val STRIPPED_TAGS = setOf(
        "size", "u", "s", "sup", "sub", "sprite", "font", "alpha", "mark", "align",
        "nobr", "link", "style", "gradient", "indent", "line-height", "cspace",
        "mspace", "voffset", "rotate", "space", "pos", "page", "width",
        "font-weight", "allcaps", "lowercase", "uppercase", "smallcaps",
    )

    private val STYLED_TAGS = setOf("b", "i", "color", "ruby", "br")

    private val NAMED_COLORS = mapOf(
        "red" to 0xFFFF0000L, "blue" to 0xFF0000FFL, "green" to 0xFF008000L,
        "yellow" to 0xFFFFFF00L, "orange" to 0xFFFFA500L, "purple" to 0xFF800080L,
        "white" to 0xFFFFFFFFL, "black" to 0xFF000000L, "grey" to 0xFF808080L,
        "gray" to 0xFF808080L, "cyan" to 0xFF00FFFFL, "magenta" to 0xFFFF00FFL,
        "brown" to 0xFFA52A2AL, "lightblue" to 0xFFADD8E6L, "teal" to 0xFF008080L,
        "olive" to 0xFF808000L, "lime" to 0xFF00FF00L, "maroon" to 0xFF800000L,
        "navy" to 0xFF000080L, "silver" to 0xFFC0C0C0L,
    )

    fun parse(raw: String): List<TextSegment> {
        if (raw.isEmpty()) return emptyList()
        if ('<' !in raw) return listOf(TextSegment(raw))

        val segments = mutableListOf<TextSegment>()
        val buffer = StringBuilder()
        var bold = 0
        var italic = 0
        val colors = ArrayDeque<Long?>()
        val rubyAnnotations = ArrayDeque<String>()

        fun flush() {
            if (buffer.isNotEmpty()) {
                segments += TextSegment(buffer.toString(), bold > 0, italic > 0, colors.lastOrNull())
                buffer.clear()
            }
        }

        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '<') {
                buffer.append(c)
                i++
                continue
            }
            val end = raw.indexOf('>', i + 1)
            val tag = if (end == -1) null else parseTag(raw.substring(i + 1, end))
            if (tag == null) {
                buffer.append(c)
                i++
                continue
            }
            when (tag.name) {
                "b" -> {
                    flush()
                    bold = if (tag.closing) (bold - 1).coerceAtLeast(0) else bold + 1
                }
                "i" -> {
                    flush()
                    italic = if (tag.closing) (italic - 1).coerceAtLeast(0) else italic + 1
                }
                "color" -> {
                    flush()
                    if (tag.closing) {
                        colors.removeLastOrNull()
                    } else {
                        // Invalid value: keep the current color so a later </color> stays balanced.
                        colors.addLast(tag.value?.let(::parseColor) ?: colors.lastOrNull())
                    }
                }
                "ruby" -> {
                    if (tag.closing) {
                        rubyAnnotations.removeLastOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { buffer.append(" (").append(it).append(')') }
                    } else if (!tag.value.isNullOrBlank()) {
                        rubyAnnotations.addLast(tag.value)
                    }
                }
                "br" -> if (!tag.closing) buffer.append('\n')
                // Recognized TMP tag with no overlay effect: drop it, keep inner text.
            }
            i = end + 1
        }
        while (rubyAnnotations.isNotEmpty()) {
            val annotation = rubyAnnotations.removeLast()
            if (annotation.isNotBlank()) buffer.append(" (").append(annotation).append(')')
        }
        flush()
        return merge(segments)
    }

    private fun parseTag(content: String): Tag? {
        var body = content.trim()
        val closing = body.startsWith("/")
        if (closing) body = body.substring(1).trim()
        val eq = body.indexOf('=')
        val name = (if (eq == -1) body else body.substring(0, eq)).trim().lowercase()
        if (!NAME_PATTERN.matches(name)) return null
        if (name !in STYLED_TAGS && name !in STRIPPED_TAGS) return null
        val value = if (eq == -1) null else body.substring(eq + 1).trim().removeSurrounding("\"")
        return Tag(name, value, closing)
    }

    private fun parseColor(value: String): Long? {
        val v = value.trim()
        if (!v.startsWith("#")) return NAMED_COLORS[v.lowercase()]
        val hex = v.substring(1)
        if (hex.any { it.digitToIntOrNull(16) == null }) return null
        return when (hex.length) {
            3 -> "FF${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}".toLong(16)
            6 -> "FF$hex".toLong(16)
            8 -> "${hex.substring(6)}${hex.substring(0, 6)}".toLong(16) // TMP is RRGGBBAA
            else -> null
        }
    }

    private fun merge(segments: List<TextSegment>): List<TextSegment> {
        val out = mutableListOf<TextSegment>()
        for (s in segments) {
            val last = out.lastOrNull()
            if (last != null && last.bold == s.bold && last.italic == s.italic && last.color == s.color) {
                out[out.lastIndex] = last.copy(text = last.text + s.text)
            } else {
                out += s
            }
        }
        return out
    }
}
