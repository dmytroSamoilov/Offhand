package com.dmytrosamoilov.offhand.core.ai.api

import kotlinx.serialization.Serializable

@Serializable
data class AvailableModel(
    val id: String,
    val displayName: String,
    val description: String,
    val modelId: String,
    val modelFile: String,
    val commitHash: String,
    val sizeInBytes: Long,
    val minTotalRamMb: Long = 0,
    val autoSelectable: Boolean = true,
    val requiresAuthToken: Boolean = false,
    val family: ModelFamily,
    val hardwareBackend: HardwareBackend,
    val maxTokens: Int,
    val topK: Int,
    val topP: Float,
    val temperature: Float,
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$modelId/resolve/$commitHash/$modelFile?download=true"
}

fun List<AvailableModel>.bestForRam(totalRamMb: Long): AvailableModel? = this
    .filter { it.autoSelectable && it.minTotalRamMb <= totalRamMb }
    .maxByOrNull { it.minTotalRamMb }

@Serializable
data class ModelCatalogFile(
    val defaultModelId: String,
    val models: List<AvailableModel>,
)
