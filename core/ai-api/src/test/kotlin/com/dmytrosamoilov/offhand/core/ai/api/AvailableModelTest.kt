package com.dmytrosamoilov.offhand.core.ai.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AvailableModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val catalogJson = """
        {
          "defaultModelId": "gemma-4-e2b",
          "models": [
            {
              "id": "gemma-4-e2b",
              "displayName": "Gemma 4 E2B",
              "description": "Gemma 4 E2B LiteRT-LM build.",
              "modelId": "litert-community/gemma-4-E2B-it-litert-lm",
              "modelFile": "gemma-4-E2B-it.litertlm",
              "commitHash": "9262660a1676eed6d0c477ab1a86344430854664",
              "sizeInBytes": 2588147712,
              "family": "GEMMA4",
              "hardwareBackend": "CPU",
              "maxTokens": 4096,
              "topK": 64,
              "topP": 0.95,
              "temperature": 1.0,
              "futureUnknownField": "ignored"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `catalog json parses with unknown keys ignored`() {
        val catalog = json.decodeFromString(ModelCatalogFile.serializer(), catalogJson)

        assertEquals("gemma-4-e2b", catalog.defaultModelId)
        assertEquals(1, catalog.models.size)
        val model = catalog.models.first()
        assertEquals(HardwareBackend.CPU, model.hardwareBackend)
        assertEquals(ModelFamily.GEMMA4, model.family)
        assertEquals(4096, model.maxTokens)
        assertEquals(2_588_147_712L, model.sizeInBytes)
    }

    @Test
    fun `download url is derived from repo, commit and file`() {
        val model = json.decodeFromString(ModelCatalogFile.serializer(), catalogJson).models.first()

        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
                "9262660a1676eed6d0c477ab1a86344430854664/gemma-4-E2B-it.litertlm?download=true",
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
        family = ModelFamily.GEMMA4,
        hardwareBackend = HardwareBackend.CPU,
        maxTokens = 4096,
        topK = 64,
        topP = 0.95f,
        temperature = 1.0f,
    )
}
