package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import io.mockk.every
import io.mockk.mockk

internal fun testModel(family: ModelFamily = ModelFamily.GEMMA4) = AvailableModel(
    id = "test-model",
    displayName = "Test model",
    description = "",
    modelId = "repo/test-model",
    modelFile = "test-model.litertlm",
    commitHash = "abc",
    sizeInBytes = 1,
    family = family,
    hardwareBackend = HardwareBackend.CPU,
    maxTokens = 4096,
    topK = 64,
    topP = 0.95f,
    temperature = 1.0f,
)

internal fun testModelManager(family: ModelFamily = ModelFamily.GEMMA4): ModelManager =
    mockk { every { model } returns testModel(family) }
