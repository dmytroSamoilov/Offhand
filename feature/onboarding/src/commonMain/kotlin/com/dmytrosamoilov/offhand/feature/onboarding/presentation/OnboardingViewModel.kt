package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.isLocalLlmCapable
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(
    private val modelDownloadController: ModelDownloadController,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val modelManager: ModelManager,
    private val appLockManager: AppLockManager,
    private val setAppLockEnabled: SetAppLockEnabledUseCase,
    private val setTelemetryConsent: SetTelemetryConsentUseCase,
    private val setNotePreset: SetNotePresetUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    private var pages: List<OnboardingStep> = emptyList()

    init {
        evaluateDevice()
    }

    fun onPrivacyContinue() {
        moveToNextPage()
    }

    fun onNoteStyleSelected(preset: NotePreset) {
        mutableUiState.update { it.copy(notePreset = preset) }
    }

    fun onNoteStyleContinue() {
        launchSafely {
            setNotePreset(uiState.value.notePreset)
            moveToNextPage()
        }
    }

    fun onAppLockToggled(enabled: Boolean) {
        mutableUiState.update { it.copy(isAppLockEnabled = enabled) }
    }

    fun onDeviceLockContinue() {
        launchSafely {
            // A device with no passcode has nothing to authenticate against, so
            // the answer is no regardless of how the toggle was left.
            val current = uiState.value
            setAppLockEnabled(current.isDeviceSecure && current.isAppLockEnabled)
            moveToNextPage()
        }
    }

    // The user may leave for system settings to add a passcode; when they come
    // back the step turns into the opt-in rather than the set-a-passcode nudge.
    fun onDeviceLockRecheck() {
        if (uiState.value.step != OnboardingStep.DEVICE_LOCK) return
        mutableUiState.update { it.copy(isDeviceSecure = appLockManager.isDeviceSecure) }
    }

    fun onTelemetryToggled(granted: Boolean) {
        mutableUiState.update { it.copy(isTelemetryEnabled = granted) }
    }

    fun onConsentContinue() {
        launchSafely {
            setTelemetryConsent(uiState.value.isTelemetryEnabled)
            moveToNextPage()
        }
    }

    fun onDownloadContinue() {
        launchSafely {
            modelDownloadController.start()
            completeOnboarding()
        }
    }

    private fun moveToNextPage() {
        val nextIndex = pages.indexOf(uiState.value.step) + 1
        val next = pages.getOrNull(nextIndex) ?: return
        mutableUiState.update { it.copy(step = next, currentPage = nextIndex) }
    }

    private fun buildPages(): List<OnboardingStep> = listOf(
        OnboardingStep.PRIVACY,
        OnboardingStep.NOTE_STYLE,
        OnboardingStep.DEVICE_LOCK,
        OnboardingStep.TELEMETRY_CONSENT,
        OnboardingStep.MODEL_DOWNLOAD,
    )

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        mutableUiState.update { current ->
            if (capability.isLocalLlmCapable()) {
                pages = buildPages()
                current.copy(
                    step = OnboardingStep.PRIVACY,
                    currentPage = 0,
                    pageCount = pages.size,
                    isDeviceSecure = appLockManager.isDeviceSecure,
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
