package com.dmytrosamoilov.offhand.feature.recording.domain

internal object ModelResponseCleaner {

    private val THINKING_BLOCK =
        Regex("<think(?:ing)?>.*?</think(?:ing)?>", RegexOption.DOT_MATCHES_ALL)

    fun stripThinking(raw: String): String = raw.replace(THINKING_BLOCK, "").trim()
}
