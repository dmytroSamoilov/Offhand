package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.isLocalLlmCapable
import com.dmytrosamoilov.offhand.core.security.AppLockManager
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
    private val appLockManager: AppLockManager,
    private val setTelemetryConsent: SetTelemetryConsentUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    init {
        evaluateDevice()
    }

    fun onPrivacyContinue() {
        mutableUiState.update { it.copy(step = nextStepAfterPrivacy()) }
    }

    fun onDeviceLockSkipped() {
        mutableUiState.update { it.copy(step = OnboardingStep.TELEMETRY_CONSENT) }
    }

    fun onDeviceLockRecheck() {
        if (uiState.value.step != OnboardingStep.DEVICE_LOCK) return
        if (!appLockManager.isDeviceSecure) return
        mutableUiState.update { it.copy(step = OnboardingStep.TELEMETRY_CONSENT) }
    }

    fun onConsentChosen(granted: Boolean) {
        launchSafely {
            setTelemetryConsent(granted)
            mutableUiState.update { it.copy(step = OnboardingStep.MODEL_DOWNLOAD) }
        }
    }

    fun onDownloadContinue() {
        launchSafely {
            ModelDownloadService.start(context)
            completeOnboarding()
        }
    }

    private fun nextStepAfterPrivacy(): OnboardingStep =
        if (appLockManager.isDeviceSecure) {
            OnboardingStep.TELEMETRY_CONSENT
        } else {
            OnboardingStep.DEVICE_LOCK
        }

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        mutableUiState.update { current ->
            if (capability.isLocalLlmCapable()) {
                current.copy(
                    step = OnboardingStep.PRIVACY,
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
