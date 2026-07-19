package com.dmytrosamoilov.offhand.core.ai.api

/**
 * Rough token estimates — LiteRT-LM does not expose per-call counts.
 * Text: ~4 characters per token for ASCII, ~2 for non-Latin scripts
 * (Cyrillic and similar tokenize far denser in SentencePiece vocabularies).
 */
object TokenEstimator {

    private const val ASCII_CHARS_PER_TOKEN = 4
    private const val NON_ASCII_CHARS_PER_TOKEN = 2
    private const val MAX_ASCII_CODE = 127

    fun approxText(text: String): Int {
        val nonAsciiChars = text.count { it.code > MAX_ASCII_CODE }
        val asciiChars = text.length - nonAsciiChars
        val asciiTokens = (asciiChars + ASCII_CHARS_PER_TOKEN - 1) / ASCII_CHARS_PER_TOKEN
        val nonAsciiTokens = (nonAsciiChars + NON_ASCII_CHARS_PER_TOKEN - 1) / NON_ASCII_CHARS_PER_TOKEN
        return asciiTokens + nonAsciiTokens
    }
}
