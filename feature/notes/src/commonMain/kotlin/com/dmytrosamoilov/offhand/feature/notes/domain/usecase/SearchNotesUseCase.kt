package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteSearchResult
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteTextCleaner
import com.dmytrosamoilov.offhand.feature.notes.domain.TextMatch

class SearchNotesUseCase {

    operator fun invoke(notes: List<Note>, query: String): List<NoteSearchResult> {
        val terms = query.trim().split(WHITESPACE_RUNS).filter(String::isNotBlank)
        if (terms.isEmpty()) return notes.map(::NoteSearchResult)
        return notes.mapNotNull { note -> match(note, terms) }
    }

    private fun match(note: Note, terms: List<String>): NoteSearchResult? {
        val body = NoteTextCleaner.clean(note.body)
        val transcript = NoteTextCleaner.clean(note.transcript)
        val fields = listOf(note.title, body, transcript)
        val allTermsFound = terms.all { term -> fields.any { it.contains(term, ignoreCase = true) } }
        if (!allTermsFound) return null
        val source = listOf(body, transcript).firstOrNull { field -> terms.any { field.contains(it, ignoreCase = true) } }
        val snippet = source?.let { snippetAround(it, terms) } ?: NoteTextCleaner.preview(body)
        return NoteSearchResult(
            note = note,
            titleMatches = occurrences(note.title, terms),
            snippet = snippet,
            snippetMatches = occurrences(snippet, terms),
        )
    }

    private fun snippetAround(text: String, terms: List<String>): String {
        val firstMatch = terms
            .map { text.indexOf(it, ignoreCase = true) }
            .filter { it >= 0 }
            .min()
        val start = wordBoundaryAfter(text, (firstMatch - LEAD_CHARS).coerceAtLeast(0))
        val end = (start + NoteTextCleaner.PREVIEW_MAX_CHARS).coerceAtMost(text.length)
        val window = text.substring(start, end)
        return if (start > 0) "$ELLIPSIS$window" else window
    }

    private fun wordBoundaryAfter(text: String, index: Int): Int {
        if (index == 0) return 0
        val nextSpace = text.indexOf(' ', index)
        return if (nextSpace in index until text.length - 1) nextSpace + 1 else index
    }

    private fun occurrences(text: String, terms: List<String>): List<TextMatch> {
        val matches = mutableListOf<TextMatch>()
        terms.forEach { term ->
            var from = text.indexOf(term, ignoreCase = true)
            while (from >= 0) {
                matches += TextMatch(start = from, end = from + term.length)
                from = text.indexOf(term, from + term.length, ignoreCase = true)
            }
        }
        return merge(matches.sortedBy { it.start })
    }

    private fun merge(sorted: List<TextMatch>): List<TextMatch> =
        sorted.fold(mutableListOf()) { acc, match ->
            val last = acc.lastOrNull()
            if (last != null && match.start <= last.end) {
                acc[acc.lastIndex] = last.copy(end = maxOf(last.end, match.end))
            } else {
                acc += match
            }
            acc
        }

    private companion object {
        val WHITESPACE_RUNS = Regex("\\s+")
        const val LEAD_CHARS = 60
        const val ELLIPSIS = "… "
    }
}
