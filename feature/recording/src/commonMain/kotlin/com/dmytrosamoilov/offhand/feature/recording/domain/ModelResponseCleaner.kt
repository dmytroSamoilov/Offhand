package com.dmytrosamoilov.offhand.feature.recording.domain

internal object ModelResponseCleaner {

    private val THINKING_BLOCK =
        Regex("<think(?:ing)?>.*?</think(?:ing)?>", RegexOption.DOT_MATCHES_ALL)

    // A response that runs out of tokens mid-thinking never closes the tag —
    // everything from the opening tag on is reasoning, not note content.
    private val UNCLOSED_THINKING =
        Regex("<think(?:ing)?>.*", RegexOption.DOT_MATCHES_ALL)

    fun stripThinking(raw: String): String = raw
        .replace(THINKING_BLOCK, "")
        .replace(UNCLOSED_THINKING, "")
        .trim()
}
