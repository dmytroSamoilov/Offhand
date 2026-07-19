package com.dmytrosamoilov.offhand.core.ai.local

internal data class WhisperFile(
    val fileName: String,
    val sizeInBytes: Long,
)

internal data class WhisperModel(
    val id: String,
    val repoId: String,
    val commitHash: String,
    val encoder: WhisperFile,
    val decoder: WhisperFile,
    val tokens: WhisperFile,
) {
    val files: List<WhisperFile> get() = listOf(encoder, decoder, tokens)

    fun downloadUrl(file: WhisperFile): String =
        "https://huggingface.co/$repoId/resolve/$commitHash/${file.fileName}?download=true"
}

internal object WhisperModels {
    const val DIRECTORY = "whisper"

    val SMALL = WhisperModel(
        id = "whisper-small-int8",
        repoId = "csukuangfj/sherpa-onnx-whisper-small",
        commitHash = "8f3c18b358db4d1f2fc1eae49d75cd20989e4309",
        encoder = WhisperFile("small-encoder.int8.onnx", 112_442_483),
        decoder = WhisperFile("small-decoder.int8.onnx", 262_226_114),
        tokens = WhisperFile("small-tokens.txt", 816_730),
    )
}
