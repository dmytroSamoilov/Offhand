package com.dmytrosamoilov.offhand.core.ai.api

import kotlinx.coroutines.flow.StateFlow

interface ModelManager {

    val model: AvailableModel

    val availableModels: List<AvailableModel>

    val speechModelSizeInBytes: Long

    val modelOverrideId: StateFlow<String?>

    val modelState: StateFlow<ModelState>

    val activeBackend: StateFlow<HardwareBackend>

    suspend fun isModelDownloaded(): Boolean

    suspend fun ensureModelAvailable()

    suspend fun setHardwareBackend(backend: HardwareBackend)

    suspend fun setModelOverride(modelId: String?)

    suspend fun deleteModel()

    fun release()
}
