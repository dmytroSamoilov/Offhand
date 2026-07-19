package com.dmytrosamoilov.offhand.core.ai.api

data class AiResult(
    val text: String,
    val processingTimeMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val hardwareBackend: HardwareBackend,
)
