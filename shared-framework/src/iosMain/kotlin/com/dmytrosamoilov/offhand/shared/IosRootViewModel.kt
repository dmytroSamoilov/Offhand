package com.dmytrosamoilov.offhand.shared

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DecideEarlyAdopterProUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkEarlyAdopterThankedUseCase
import com.dmytrosamoilov.offhand.core.data.domain.isEarlyAdopterThanksPending
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

enum class IosRootPhase {
    LOADING,
    ONBOARDING,
    LOCKED,
    READY,
}

class IosRootViewModel(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val appLockManager: AppLockManager,
    private val modelManager: ModelManager,
    private val modelDownloadLauncher: ModelDownloadLauncher,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
    private val sweepOrphanedRecordings: SweepOrphanedRecordingsUseCase,
    private val clearShareCache: ClearShareCacheUseCase,
    private val decideEarlyAdopterPro: DecideEarlyAdopterProUseCase,
    private val markEarlyAdopterThanked: MarkEarlyAdopterThankedUseCase,
    private val proUpgradeGate: ProUpgradeGate,
    pendingNotesCoordinator: PendingNotesCoordinator,
) : BaseViewModel() {

    val phase: StateFlow<IosRootPhase> = combine(
        observeUserPreferences(),
        appLockManager.lockState,
    ) { preferences, lockState ->
        when {
            !preferences.onboardingCompleted -> IosRootPhase.ONBOARDING
            preferences.appLockEnabled && lockState == AppLockState.LOCKED -> IosRootPhase.LOCKED
            else -> IosRootPhase.READY
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, IosRootPhase.LOADING)

    val isEarlyAdopterThanksShown: StateFlow<Boolean> = combine(phase, observeUserPreferences()) { phase, preferences ->
        phase == IosRootPhase.READY && preferences.isEarlyAdopterThanksPending
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isDeviceSecure: Boolean
        get() = appLockManager.isDeviceSecure

    init {
        skipLockForFirstRun(observeUserPreferences)
        finishStartupWhenReady()
        resumeModelDownloadWhenReady()
        // Picks notes back up the moment the model download finishes, instead of
        // leaving them stuck until the app is backgrounded and reopened.
        pendingNotesCoordinator.start()
    }

    fun onEarlyAdopterThanksDismissed() {
        launchSafely(showLoading = false) { markEarlyAdopterThanked() }
    }

    // Called once the thank-you sheet is gone: a sheet cannot present the
    // paywall cover itself.
    fun onProBenefitsRequested() {
        proUpgradeGate.showBenefits()
    }

    fun onUnlockAuthenticated() {
        appLockManager.markUnlocked()
    }

    // Called when the app backgrounds while no recording is in flight; the caller
    // owns the recording check because session state lives outside this module.
    fun lockOnBackground() {
        appLockManager.markLocked()
    }

    // Runs on every return to the foreground, so a download that the system
    // dropped while the app was away is picked up again without a relaunch.
    fun onReady() {
        launchSafely(showLoading = false) {
            if (phase.value != IosRootPhase.READY) return@launchSafely
            resumeInterruptedNotes()
            startModelDownloadIfMissing()
        }
    }

    private fun finishStartupWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            decideEarlyAdopterPro()
            resumeInterruptedNotes()
            sweepOrphanedRecordings()
            clearShareCache()
        }
    }

    // READY implies onboarding is complete, so the user has already agreed to the download.
    private fun resumeModelDownloadWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            startModelDownloadIfMissing()
        }
    }

    private suspend fun startModelDownloadIfMissing() {
        if (!modelManager.isModelDownloaded()) {
            modelDownloadLauncher.startWhenAllowed()
        }
    }

    private fun skipLockForFirstRun(observeUserPreferences: ObserveUserPreferencesUseCase) {
        launchSafely(showLoading = false) {
            if (!observeUserPreferences().first().onboardingCompleted) {
                appLockManager.markUnlocked()
                decideEarlyAdopterPro()
            }
        }
    }
}
