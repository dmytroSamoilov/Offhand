package com.dmytrosamoilov.offhand.core.ai.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AvailableModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val catalogJson = """
        {
          "defaultModelId": "qwen3-1.7b",
          "models": [
            {
              "id": "qwen3-1.7b",
              "displayName": "Qwen3 1.7B",
              "description": "Qwen3 1.7B LiteRT-LM build.",
              "modelId": "litert-community/Qwen3-1.7B",
              "modelFile": "Qwen3_1.7B.litertlm",
              "commitHash": "5d8fd1f27c771dbbbb185c9f05c3547760dd3cbd",
              "sizeInBytes": 2056729520,
              "family": "QWEN3",
              "hardwareBackend": "CPU",
              "maxTokens": 4096,
              "topK": 20,
              "topP": 0.95,
              "temperature": 0.6,
              "futureUnknownField": "ignored"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `catalog json parses with unknown keys ignored`() {
        val catalog = json.decodeFromString(ModelCatalogFile.serializer(), catalogJson)

        assertEquals("qwen3-1.7b", catalog.defaultModelId)
        assertEquals(1, catalog.models.size)
        val model = catalog.models.first()
        assertEquals(HardwareBackend.CPU, model.hardwareBackend)
        assertEquals(ModelFamily.QWEN3, model.family)
        assertEquals(false, model.requiresAuthToken)
        assertEquals(4096, model.maxTokens)
        assertEquals(2_056_729_520L, model.sizeInBytes)
    }

    @Test
    fun `download url is derived from repo, commit and file`() {
        val model = json.decodeFromString(ModelCatalogFile.serializer(), catalogJson).models.first()

        assertEquals(
            "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/" +
                "5d8fd1f27c771dbbbb185c9f05c3547760dd3cbd/Qwen3_1.7B.litertlm?download=true",
            model.downloadUrl,
        )
    }

    @Test
    fun `bestForRam picks the largest model the device can hold`() {
        val small = model(id = "small", minTotalRamMb = 0)
        val large = model(id = "large", minTotalRamMb = 9_216)
        val models = listOf(small, large)

        assertEquals(small, models.bestForRam(7_400))
        assertEquals(large, models.bestForRam(9_216))
        assertEquals(large, models.bestForRam(11_264))
    }

    @Test
    fun `bestForRam returns null when no model fits`() {
        val models = listOf(model(id = "large", minTotalRamMb = 9_216))

        assertEquals(null, models.bestForRam(7_400))
    }

    private fun model(id: String, minTotalRamMb: Long) = AvailableModel(
        id = id,
        displayName = id,
        description = "",
        modelId = "repo/$id",
        modelFile = "$id.litertlm",
        commitHash = "abc",
        sizeInBytes = 1,
        minTotalRamMb = minTotalRamMb,
        family = ModelFamily.QWEN3,
        hardwareBackend = HardwareBackend.CPU,
        maxTokens = 4096,
        topK = 20,
        topP = 0.95f,
        temperature = 0.6f,
    )
}
