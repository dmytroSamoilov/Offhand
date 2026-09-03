package com.dmytrosamoilov.offhand.feature.recording.domain

internal object NoteProseFormatter {

    fun format(overviews: List<String>): String {
        val paragraphs = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        overviews.flatMap { it.lines() + PARAGRAPH_BREAK }.forEach { raw ->
            val line = stripMarkers(raw)
            when {
                line.isBlank() -> appendBreak(paragraphs)
                seen.add(line) -> paragraphs += line
            }
        }
        return paragraphs.joinToString(LINE_BREAK).trim()
    }

    private fun appendBreak(paragraphs: MutableList<String>) {
        if (paragraphs.isNotEmpty() && paragraphs.last().isNotBlank()) {
            paragraphs += PARAGRAPH_BREAK
        }
    }

    private fun stripMarkers(line: String): String = line
        .trim()
        .replace(HEADING_MARKER, "")
        .replace(LIST_MARKER, "")
        .trim()

    private const val PARAGRAPH_BREAK = ""
    private const val LINE_BREAK = "\n"
    private val HEADING_MARKER = Regex("^#{1,6}\\s*")
    private val LIST_MARKER = Regex("^(?:[-*•]|\\d+[.)])\\s+")
}
