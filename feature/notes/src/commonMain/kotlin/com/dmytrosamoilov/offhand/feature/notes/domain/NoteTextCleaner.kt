package com.dmytrosamoilov.offhand.feature.notes.domain

object NoteTextCleaner {

    private val MARKDOWN_TOKENS = Regex("[#*>`_\\[\\]]")
    private val WHITESPACE_RUNS = Regex("\\s+")
    const val PREVIEW_MAX_CHARS = 220

    fun clean(text: String): String = text
        .replace(MARKDOWN_TOKENS, " ")
        .replace(WHITESPACE_RUNS, " ")
        .trim()

    fun preview(text: String): String = clean(text).take(PREVIEW_MAX_CHARS)
}
