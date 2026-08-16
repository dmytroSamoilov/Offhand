package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily

internal val gemmaE2bModel = AvailableModel(
    id = "gemma-4-e2b",
    displayName = "Gemma 4 E2B",
    description = "On-device model that structures your recordings into notes",
    modelId = "litert-community/gemma-4-E2B-it-litert-lm",
    modelFile = "gemma-4-E2B-it.litertlm",
    commitHash = "9262660a1676eed6d0c477ab1a86344430854664",
    sizeInBytes = 2_588_147_712,
    family = ModelFamily.GEMMA4,
    hardwareBackend = HardwareBackend.CPU,
    maxTokens = 4_096,
    topK = 40,
    topP = 0.9f,
    temperature = 0.4f,
)

internal class WhisperFile(val fileName: String, val sizeInBytes: Long) {
    val downloadUrl: String
        get() = "https://huggingface.co/$WHISPER_REPO/resolve/$WHISPER_COMMIT/$fileName"
}

internal val whisperFiles = listOf(
    WhisperFile("small-encoder.int8.onnx", 112_442_483),
    WhisperFile("small-decoder.int8.onnx", 262_226_114),
    WhisperFile("small-tokens.txt", 816_730),
)

private const val WHISPER_REPO = "csukuangfj/sherpa-onnx-whisper-small"
private const val WHISPER_COMMIT = "8f3c18b358db4d1f2fc1eae49d75cd20989e4309"
