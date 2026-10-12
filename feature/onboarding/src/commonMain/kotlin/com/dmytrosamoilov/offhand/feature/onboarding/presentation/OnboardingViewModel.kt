package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.isLocalLlmCapable
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Three pages, then the app: the on-device AI download starts by itself the
// moment the device check passes (on Wi-Fi, or on mobile data once the user
// asks for it from the notes list), and notifications are asked for after the
// first recording instead of here.
class OnboardingViewModel(
    private val modelDownloadLauncher: ModelDownloadLauncher,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val appLockManager: AppLockManager,
    private val setAppLockEnabled: SetAppLockEnabledUseCase,
    private val setTelemetryConsent: SetTelemetryConsentUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    init {
        evaluateDevice()
    }

    fun onPrivacyContinue() {
        trackStep(OnboardingStep.PRIVACY)
        moveToNextPage()
    }

    fun onAppLockToggled(enabled: Boolean) {
        mutableUiState.update { it.copy(isAppLockEnabled = enabled) }
    }

    fun onDeviceLockContinue() {
        commitAndMoveOn(OnboardingStep.DEVICE_LOCK)
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

    // The last page: its Continue is the end of onboarding.
    fun onConsentContinue() {
        launchSafely {
            commitStep(OnboardingStep.TELEMETRY_CONSENT)
            trackStep(OnboardingStep.TELEMETRY_CONSENT)
            completeOnboarding()
            analyticsTracker.track(AnalyticsEvents.onboardingCompleted())
        }
    }

    // Swiping back through the wizard is free; swiping forward only reaches
    // pages already unlocked with Continue, and re-persists every page crossed
    // so a choice changed on a revisited page is never silently dropped.
    fun onPageSelected(page: Int) {
        launchSafely {
            val current = uiState.value
            val target = page.coerceIn(0, current.furthestPage)
            if (current.pages.isEmpty() || target == current.currentPage) return@launchSafely
            (current.currentPage until target).forEach { index ->
                commitStep(current.pages[index])
            }
            mutableUiState.update { it.copy(step = it.pages[target], currentPage = target) }
        }
    }

    private fun commitAndMoveOn(step: OnboardingStep) {
        launchSafely {
            commitStep(step)
            trackStep(step)
            moveToNextPage()
        }
    }

    private fun trackStep(step: OnboardingStep) {
        analyticsTracker.track(AnalyticsEvents.onboardingStep(step.name.lowercase()))
    }

    private suspend fun commitStep(step: OnboardingStep) {
        val current = uiState.value
        when (step) {
            // A device with no passcode has nothing to authenticate against, so
            // the answer is no regardless of how the toggle was left.
            OnboardingStep.DEVICE_LOCK ->
                setAppLockEnabled(current.isDeviceSecure && current.isAppLockEnabled)
            OnboardingStep.TELEMETRY_CONSENT -> setTelemetryConsent(current.isTelemetryEnabled)
            else -> Unit
        }
    }

    private fun moveToNextPage() {
        mutableUiState.update { current ->
            val nextIndex = current.currentPage + 1
            val next = current.pages.getOrNull(nextIndex) ?: return@update current
            current.copy(
                step = next,
                currentPage = nextIndex,
                furthestPage = maxOf(current.furthestPage, nextIndex),
            )
        }
    }

    private fun buildPages(): List<OnboardingStep> = listOf(
        OnboardingStep.PRIVACY,
        OnboardingStep.DEVICE_LOCK,
        OnboardingStep.TELEMETRY_CONSENT,
    )

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        mutableUiState.update { current ->
            if (capability.isLocalLlmCapable()) {
                modelDownloadLauncher.startIfAllowed()
                current.copy(
                    step = OnboardingStep.PRIVACY,
                    pages = buildPages(),
                    currentPage = 0,
                    furthestPage = 0,
                    isDeviceSecure = appLockManager.isDeviceSecure,
                )
            } else {
                analyticsTracker.track(
                    AnalyticsEvents.deviceUnsupported(capability.totalRamMb / MB_PER_GB, capability.cpuCores),
                )
                current.copy(
                    step = OnboardingStep.DEVICE_INCOMPATIBLE,
                    deviceSpecs = capability.toDeviceSpecsUi(),
                )
            }
        }
    }
}

private const val MB_PER_GB = 1024L
