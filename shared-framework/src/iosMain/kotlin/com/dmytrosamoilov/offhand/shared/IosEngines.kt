package com.dmytrosamoilov.offhand.shared

interface IosGemmaEngine {

    suspend fun load(
        modelPath: String,
        useGpu: Boolean,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
    )

    suspend fun generate(systemPrompt: String, userText: String): String

    fun unload()
}

interface IosWhisperEngine {

    suspend fun prepare(encoderPath: String, decoderPath: String, tokensPath: String)

    suspend fun transcribe(wavBytes: ByteArray): String

    fun releaseEngine()
}

interface IosAudioSource {

    fun start(
        onFrame: (ShortArray) -> Unit,
        onInputChanged: (String?) -> Unit,
        onFailure: (String) -> Unit,
    ): Boolean

    fun stop()

    fun hasPermission(): Boolean
}
