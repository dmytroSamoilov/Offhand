package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceTier
import com.dmytrosamoilov.offhand.core.device.tier
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.AnswerTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.GetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.IsBatteryHintNeededUseCase
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
    private val getTelemetryConsent: GetTelemetryConsentUseCase,
    private val answerTelemetryConsent: AnswerTelemetryConsentUseCase,
    private val isBatteryHintNeeded: IsBatteryHintNeededUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    init {
        launchSafely(showLoading = false) {
            if (getTelemetryConsent().isAnswered) {
                evaluateDevice()
            } else {
                mutableUiState.update { it.copy(step = OnboardingStep.ANALYTICS_CONSENT) }
            }
        }
    }

    fun onShareAnalyticsClicked() {
        answerAnalytics(isShared = true)
    }

    fun onDontShareAnalyticsClicked() {
        answerAnalytics(isShared = false)
    }

    fun onContinueWithTranscriptsClicked() {
        if (uiState.value.step != OnboardingStep.TRANSCRIPTS_ONLY) return
        analyticsTracker.track(AnalyticsEvents.onboardingStep(OnboardingChoice.TRANSCRIPTS_ONLY.analyticsName))
        mutableUiState.update { it.copy(step = stepBeforeRecording()) }
    }

    fun onBatterySettingsOpened() {
        leaveBatteryHint(OnboardingChoice.BATTERY_SETTINGS)
    }

    fun onBatteryHintSkipped() {
        leaveBatteryHint(OnboardingChoice.BATTERY_SKIPPED)
    }

    private fun leaveBatteryHint(choice: OnboardingChoice) {
        if (uiState.value.step != OnboardingStep.BACKGROUND_ACTIVITY) return
        analyticsTracker.track(AnalyticsEvents.onboardingStep(choice.analyticsName))
        mutableUiState.update { it.copy(step = OnboardingStep.WELCOME) }
    }

    private fun stepBeforeRecording(): OnboardingStep =
        if (isBatteryHintNeeded()) OnboardingStep.BACKGROUND_ACTIVITY else OnboardingStep.WELCOME

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

    private fun answerAnalytics(isShared: Boolean) {
        if (uiState.value.step != OnboardingStep.ANALYTICS_CONSENT) return
        mutableUiState.update { it.copy(step = OnboardingStep.DEVICE_CHECK) }
        launchSafely(showLoading = false) {
            answerTelemetryConsent(isShared)
            if (isShared) {
                analyticsTracker.track(AnalyticsEvents.onboardingStep(OnboardingChoice.ANALYTICS_SHARED.analyticsName))
            }
            evaluateDevice()
        }
    }

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        when (capability.tier()) {
            DeviceTier.FULL -> startWith(stepBeforeRecording(), isSummaryAvailable = true)
            DeviceTier.TRANSCRIPTS_ONLY -> {
                analyticsTracker.track(AnalyticsEvents.deviceTranscriptsOnly(capability.ramGb(), capability.cpuCores))
                startWith(OnboardingStep.TRANSCRIPTS_ONLY, isSummaryAvailable = false)
            }
            DeviceTier.UNSUPPORTED -> {
                analyticsTracker.track(AnalyticsEvents.deviceUnsupported(capability.ramGb(), capability.cpuCores))
                mutableUiState.update {
                    it.copy(step = OnboardingStep.DEVICE_INCOMPATIBLE, deviceSpecs = capability.toDeviceSpecsUi())
                }
            }
        }
    }

    private fun startWith(step: OnboardingStep, isSummaryAvailable: Boolean) {
        modelDownloadLauncher.startIfAllowed()
        mutableUiState.update { it.copy(step = step, isSummaryAvailable = isSummaryAvailable) }
    }

    private fun DeviceCapability.ramGb(): Long = totalRamMb / MB_PER_GB
}

private const val MB_PER_GB = 1024L
