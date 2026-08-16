package com.dmytrosamoilov.offhand.core.ai.api

interface AiBackend {

    suspend fun prewarm()

    suspend fun processText(systemPrompt: String, userText: String): AiResult
}

class AiBackendException(message: String, cause: Throwable? = null) : Exception(message, cause)
