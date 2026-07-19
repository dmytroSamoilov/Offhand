package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.isLocalLlmCapable
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.service.ModelDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val modelManager: ModelManager,
    private val setTelemetryConsent: SetTelemetryConsentUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    init {
        evaluateDevice()
    }

    fun onDownloadContinue() {
        ModelDownloadService.start(context)
        mutableUiState.update { it.copy(step = OnboardingStep.TELEMETRY_CONSENT) }
    }

    fun onConsentChosen(granted: Boolean) {
        launchSafely {
            setTelemetryConsent(granted)
            completeOnboarding()
        }
    }

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        mutableUiState.update { current ->
            if (capability.isLocalLlmCapable()) {
                current.copy(
                    step = OnboardingStep.MODEL_DOWNLOAD,
                    downloadSizeGb = formatDownloadSizeGb(
                        modelManager.model.sizeInBytes + modelManager.speechModelSizeInBytes,
                    ),
                )
            } else {
                current.copy(
                    step = OnboardingStep.DEVICE_INCOMPATIBLE,
                    deviceSpecs = capability.toDeviceSpecsUi(),
                )
            }
        }
    }
}
