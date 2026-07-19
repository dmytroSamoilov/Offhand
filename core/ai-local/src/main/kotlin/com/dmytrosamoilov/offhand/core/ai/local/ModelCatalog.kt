package com.dmytrosamoilov.offhand.core.ai.local

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.ModelCatalogFile
import com.dmytrosamoilov.offhand.core.ai.api.bestForRam
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class ModelCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalog: ModelCatalogFile by lazy {
        context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
            json.decodeFromString(ModelCatalogFile.serializer(), reader.readText())
        }
    }

    val all: List<AvailableModel>
        get() = catalog.models

    val modelForDevice: AvailableModel by lazy {
        catalog.models.bestForRam(deviceCapabilityChecker.snapshot().totalRamMb)
            ?: fallback
    }

    private val fallback: AvailableModel
        get() = catalog.models.firstOrNull { it.id == catalog.defaultModelId }
            ?: catalog.models.firstOrNull()
            ?: error("Catalog has no models")

    private companion object {
        const val ASSET_PATH = "models.json"
    }
}
