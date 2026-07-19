package com.dmytrosamoilov.offhand.feature.settings.presentation

data class SettingsUiState(
    val selectedBackend: BackendOptionUi = BackendOptionUi.CPU,
    val model: ModelUi = ModelUi(),
    val modelOptions: List<ModelOptionUi> = emptyList(),
    val selectedModelId: String? = null,
    val isTelemetryEnabled: Boolean = false,
    val isDynamicColorEnabled: Boolean = false,
    val isDeveloperSectionVisible: Boolean = false,
    val isDeveloperOptionsEnabled: Boolean = false,
    val isDeleteModelConfirmationVisible: Boolean = false,
)

data class ModelOptionUi(
    val id: String,
    val displayName: String,
    val sizeGb: String,
    val description: String,
)

enum class BackendOptionUi {
    CPU,
    GPU,
}

data class ModelUi(
    val displayName: String = "",
    val sizeGb: String = "",
    val status: ModelStatusUi = ModelStatusUi.NOT_DOWNLOADED,
    val downloadPercent: Int = 0,
    val errorMessage: String? = null,
)

enum class ModelStatusUi {
    NOT_DOWNLOADED,
    DOWNLOADING,
    LOADING,
    READY,
    ERROR,
}
