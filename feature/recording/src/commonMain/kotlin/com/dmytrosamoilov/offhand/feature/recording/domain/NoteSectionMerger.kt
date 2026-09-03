package com.dmytrosamoilov.offhand.feature.recording.domain

internal object NoteSectionMerger {

    fun merge(overviews: List<String>, headings: List<String>): String {
        val sections = linkedMapOf(PREAMBLE to mutableListOf<String>())
        headings.forEach { heading -> sections[heading] = mutableListOf() }
        overviews.forEach { overview -> collect(overview, headings, sections) }
        dropLinesRepeatedInLaterSections(sections)
        return sections.entries
            .filter { it.value.isNotEmpty() }
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
        sections: MutableMap<String, MutableList<String>>,
    ) {
        var current = PREAMBLE
        overview.lines().map(String::trim).filter(String::isNotEmpty).forEach { line ->
            val heading = headingOf(line, headings)
            if (heading != null) {
                current = heading
                sections.getOrPut(heading) { mutableListOf() }
            } else {
                sections.getValue(current).addIfAbsent(normalizeListMarkers(line))
            }
        }
    }

    private fun headingOf(line: String, headings: List<String>): String? {
        if (!line.startsWith(HEADING_MARKER)) return null
        val text = line.trimStart('#', ' ').trim()
        return headings.firstOrNull { it.removePrefix(HEADING_PREFIX).equals(text, ignoreCase = true) }
            ?: "$HEADING_PREFIX$text"
    }

    private fun render(heading: String, lines: List<String>): String =
        if (heading == PREAMBLE) {
            lines.joinToString(LINE_BREAK)
        } else {
            heading + LINE_BREAK + lines.joinToString(LINE_BREAK)
        }

    private fun normalizeListMarkers(line: String): String =
        line.replace(LIST_MARKER_RUN) { "$LIST_MARKER " }

    private fun MutableList<String>.addIfAbsent(line: String) {
        if (line !in this) add(line)
    }

    private const val PREAMBLE = ""
    private const val HEADING_MARKER = "#"
    private const val HEADING_PREFIX = "## "
    private const val LIST_MARKER = "-"
    private const val LINE_BREAK = "\n"
    private const val SECTION_SEPARATOR = "\n\n"
    private const val MIN_SHARED_WORDS = 4
    private const val CONTAINMENT_THRESHOLD = 0.7
    private val LIST_MARKER_RUN = Regex("^(?:[-*•]\\s+)+")
}
