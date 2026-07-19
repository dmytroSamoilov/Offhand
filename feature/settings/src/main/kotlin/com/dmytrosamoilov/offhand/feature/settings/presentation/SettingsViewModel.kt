package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetTelemetryConsentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val modelManager: ModelManager,
    observeTelemetryConsent: ObserveTelemetryConsentUseCase,
    private val setTelemetryConsent: SetTelemetryConsentUseCase,
    observeDynamicColor: ObserveDynamicColorUseCase,
    private val setDynamicColor: SetDynamicColorUseCase,
    observeDeveloperOptions: ObserveDeveloperOptionsUseCase,
    private val setDeveloperOptions: SetDeveloperOptionsUseCase,
    buildInfo: BuildInfo,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        mutableUiState.update {
            it.copy(
                isDeveloperSectionVisible = buildInfo.isDebugBuild,
                model = it.model.copy(
                    displayName = modelManager.model.displayName,
                    sizeGb = formatGb(modelManager.model.sizeInBytes),
                ),
                modelOptions = modelManager.availableModels.map { available ->
                    ModelOptionUi(
                        id = available.id,
                        displayName = available.displayName,
                        sizeGb = formatGb(available.sizeInBytes),
                        description = available.description,
                    )
                },
            )
        }
        viewModelScope.launch {
            modelManager.modelOverrideId.collect { overrideId ->
                mutableUiState.update {
                    it.copy(
                        selectedModelId = overrideId,
                        model = it.model.copy(
                            displayName = modelManager.model.displayName,
                            sizeGb = formatGb(modelManager.model.sizeInBytes),
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            modelManager.activeBackend.collect { backend ->
                mutableUiState.update { it.copy(selectedBackend = backend.toOptionUi()) }
            }
        }
        viewModelScope.launch {
            modelManager.modelState.collect { state ->
                mutableUiState.update { it.copy(model = it.model.applyState(state)) }
            }
        }
        viewModelScope.launch {
            observeTelemetryConsent().collect { granted ->
                mutableUiState.update { it.copy(isTelemetryEnabled = granted) }
            }
        }
        viewModelScope.launch {
            observeDynamicColor().collect { enabled ->
                mutableUiState.update { it.copy(isDynamicColorEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            observeDeveloperOptions().collect { enabled ->
                mutableUiState.update { it.copy(isDeveloperOptionsEnabled = enabled) }
            }
        }
    }

    fun onBackendSelected(option: BackendOptionUi) {
        launchSafely(showLoading = false) {
            modelManager.setHardwareBackend(option.toDomain())
        }
    }

    fun onModelSelected(modelId: String?) {
        launchSafely(showLoading = false) {
            modelManager.setModelOverride(modelId)
        }
    }

    fun onTelemetryChanged(granted: Boolean) {
        launchSafely(showLoading = false) {
            setTelemetryConsent(granted)
        }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            setDynamicColor(enabled)
        }
    }

    fun onDeveloperOptionsChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            setDeveloperOptions(enabled)
        }
    }

    fun onDownloadModel() {
        launchSafely(showLoading = false) {
            modelManager.ensureModelAvailable()
        }
    }

    fun onDeleteModelRequested() {
        mutableUiState.update { it.copy(isDeleteModelConfirmationVisible = true) }
    }

    fun onDeleteModelDismissed() {
        mutableUiState.update { it.copy(isDeleteModelConfirmationVisible = false) }
    }

    fun onDeleteModelConfirmed() {
        launchSafely {
            modelManager.deleteModel()
            mutableUiState.update { it.copy(isDeleteModelConfirmationVisible = false) }
        }
    }

    private fun formatGb(bytes: Long): String =
        String.format(Locale.US, "%.1f", bytes / (1024f * 1024f * 1024f))
}

private fun HardwareBackend.toOptionUi(): BackendOptionUi = when (this) {
    HardwareBackend.GPU -> BackendOptionUi.GPU
    HardwareBackend.CPU, HardwareBackend.NPU -> BackendOptionUi.CPU
}

private fun BackendOptionUi.toDomain(): HardwareBackend = when (this) {
    BackendOptionUi.CPU -> HardwareBackend.CPU
    BackendOptionUi.GPU -> HardwareBackend.GPU
}

private fun ModelUi.applyState(state: ModelState): ModelUi = when (state) {
    is ModelState.NotDownloaded -> copy(
        status = ModelStatusUi.NOT_DOWNLOADED,
        downloadPercent = 0,
        errorMessage = null,
    )
    is ModelState.Downloading -> copy(
        status = ModelStatusUi.DOWNLOADING,
        downloadPercent = (state.progress * 100).toInt().coerceIn(0, 100),
        errorMessage = null,
    )
    is ModelState.Loading -> copy(status = ModelStatusUi.LOADING, errorMessage = null)
    is ModelState.Downloaded -> copy(status = ModelStatusUi.READY, errorMessage = null)
    is ModelState.Ready -> copy(status = ModelStatusUi.READY, errorMessage = null)
    is ModelState.Error -> copy(status = ModelStatusUi.ERROR, errorMessage = state.message)
}
