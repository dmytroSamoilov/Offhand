package com.dmytrosamoilov.offhand.feature.recording.domain

internal object NoteSectionMerger {

    // Prose sections are the user's own text: their paragraph breaks are kept
    // and their lines are never de-duplicated, because a repeated line there
    // is what was said, not a merge artefact.
    fun merge(overviews: List<String>, headings: List<String>, proseHeadings: List<String> = emptyList()): String {
        val sections = linkedMapOf(PREAMBLE to mutableListOf<String>())
        headings.forEach { heading -> sections[heading] = mutableListOf() }
        overviews.forEach { overview -> collect(overview, headings, proseHeadings, sections) }
        dropLinesRepeatedInLaterSections(sections)
        return sections.entries
            .filter { it.value.any(String::isNotEmpty) }
            .joinToString(SECTION_SEPARATOR) { (heading, lines) -> render(heading, lines) }
    }

    private fun dropLinesRepeatedInLaterSections(sections: MutableMap<String, MutableList<String>>) {
        val ordered = sections.values.toList()
        ordered.forEachIndexed { index, lines ->
            val laterLines = ordered.drop(index + 1).flatten()
            lines.removeAll { line -> laterLines.any { later -> isSameStatement(line, later) } }
        }
    }

    private fun isSameStatement(first: String, second: String): Boolean {
        val firstWords = contentWords(first)
        val secondWords = contentWords(second)
        val shared = firstWords.intersect(secondWords).size
        val smaller = minOf(firstWords.size, secondWords.size)
        return smaller > 0 &&
            shared >= MIN_SHARED_WORDS &&
            shared >= smaller * CONTAINMENT_THRESHOLD
    }

    private fun contentWords(line: String): Set<String> = buildSet {
        val word = StringBuilder()
        for (character in line.lowercase()) {
            if (character.isLetterOrDigit()) {
                word.append(character)
            } else if (word.isNotEmpty()) {
                add(word.toString())
                word.clear()
            }
        }
        if (word.isNotEmpty()) add(word.toString())
    }

    private fun collect(
        overview: String,
        headings: List<String>,
        proseHeadings: List<String>,
        sections: MutableMap<String, MutableList<String>>,
    ) {
        var current = PREAMBLE
        var startsParagraph = true
        overview.lines().map(String::trim).forEach { line ->
            val heading = headingOf(line, headings)
            when {
                heading != null -> {
                    current = heading
                    startsParagraph = true
                    sections.getOrPut(heading) { mutableListOf() }
                }
                current !in proseHeadings -> if (line.isNotEmpty()) sections.getValue(current).addIfAbsent(normalizeListMarkers(line))
                line.isEmpty() -> startsParagraph = true
                else -> {
                    sections.getValue(current).addProseLine(line, startsParagraph)
                    startsParagraph = false
                }
            }
        }
    }

    private fun headingOf(line: String, headings: List<String>): String? {
        if (!line.startsWith(HEADING_MARKER)) return null
        val text = line.trimStart('#', ' ').trim()
        return headings.firstOrNull { it.removePrefix(HEADING_PREFIX).equals(text, ignoreCase = true) }
            ?: "$HEADING_PREFIX$text"
    }

    private fun render(heading: String, lines: List<String>): String {
        val body = lines.dropLastWhile(String::isEmpty).joinToString(LINE_BREAK)
        return if (heading == PREAMBLE) body else heading + LINE_BREAK + body
    }

    private fun normalizeListMarkers(line: String): String =
        line.replace(LIST_MARKER_RUN) { "$LIST_MARKER " }

    private fun MutableList<String>.addIfAbsent(line: String) {
        if (line !in this) add(line)
    }

    private fun MutableList<String>.addProseLine(line: String, startsParagraph: Boolean) {
        if (startsParagraph && isNotEmpty()) add(PARAGRAPH_BREAK)
        add(line)
    }

    private const val PREAMBLE = ""
    private const val PARAGRAPH_BREAK = ""
    private const val HEADING_MARKER = "#"
    private const val HEADING_PREFIX = "## "
    private const val LIST_MARKER = "-"
    private const val LINE_BREAK = "\n"
    private const val SECTION_SEPARATOR = "\n\n"
    private const val MIN_SHARED_WORDS = 4
    private const val CONTAINMENT_THRESHOLD = 0.7
    private val LIST_MARKER_RUN = Regex("^(?:[-*•]\\s+)+")
}
