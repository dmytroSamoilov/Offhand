package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeModelManager : ModelManager {

    override val model: AvailableModel = FAKE_MODEL
    override val availableModels: List<AvailableModel> = listOf(FAKE_MODEL)
    override val speechModelSizeInBytes: Long = 0L

    private val mutableOverrideId = MutableStateFlow<String?>(null)
    override val modelOverrideId: StateFlow<String?> = mutableOverrideId.asStateFlow()

    private val mutableModelState = MutableStateFlow<ModelState>(ModelState.Ready)
    override val modelState: StateFlow<ModelState> = mutableModelState.asStateFlow()

    private val mutableBackend = MutableStateFlow(HardwareBackend.CPU)
    override val activeBackend: StateFlow<HardwareBackend> = mutableBackend.asStateFlow()

    override suspend fun isModelDownloaded(): Boolean = mutableModelState.value != ModelState.NotDownloaded

    override suspend fun ensureModelAvailable() {
        mutableModelState.value = ModelState.Ready
    }

    override suspend fun setHardwareBackend(backend: HardwareBackend) {
        mutableBackend.value = backend
    }

    override suspend fun setModelOverride(modelId: String?) {
        mutableOverrideId.value = modelId
    }

    override suspend fun deleteModel() {
        mutableModelState.value = ModelState.NotDownloaded
    }

    override fun release() = Unit

    private companion object {
        val FAKE_MODEL = AvailableModel(
            id = "fake-gemma",
            displayName = "Fake Gemma (smoke tests)",
            description = "Returns canned answers instantly.",
            modelId = "offhand/fake",
            modelFile = "fake.litertlm",
            commitHash = "0",
            sizeInBytes = 0L,
            family = ModelFamily.GEMMA4,
            hardwareBackend = HardwareBackend.CPU,
            maxTokens = 8192,
            topK = 40,
            topP = 0.95f,
            temperature = 0.7f,
        )
    }
}
