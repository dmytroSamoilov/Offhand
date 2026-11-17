package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.isLocalLlmCapable
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(
    private val modelDownloadLauncher: ModelDownloadLauncher,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val setAppLockEnabled: SetAppLockEnabledUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    init {
        evaluateDevice()
    }

    fun onRecordFirstNoteClicked() {
        finish(OnboardingChoice.RECORD_FIRST_NOTE)
    }

    fun onLookAroundClicked() {
        finish(OnboardingChoice.LOOK_AROUND)
    }

    private fun finish(choice: OnboardingChoice) {
        if (uiState.value.step != OnboardingStep.WELCOME) return
        launchSafely {
            analyticsTracker.track(AnalyticsEvents.onboardingStep(choice.analyticsName))
            setAppLockEnabled(false)
            completeOnboarding()
            analyticsTracker.track(AnalyticsEvents.onboardingCompleted())
        }
    }

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        if (capability.isLocalLlmCapable()) {
            modelDownloadLauncher.startIfAllowed()
            mutableUiState.update { it.copy(step = OnboardingStep.WELCOME) }
            return
        }
        analyticsTracker.track(
            AnalyticsEvents.deviceUnsupported(capability.totalRamMb / MB_PER_GB, capability.cpuCores),
        )
        mutableUiState.update {
            it.copy(step = OnboardingStep.DEVICE_INCOMPATIBLE, deviceSpecs = capability.toDeviceSpecsUi())
        }
    }
}

private const val MB_PER_GB = 1024L
