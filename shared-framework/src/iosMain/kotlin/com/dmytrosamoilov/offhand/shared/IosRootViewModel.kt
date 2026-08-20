package com.dmytrosamoilov.offhand.shared

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
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
    private val modelDownloadController: ModelDownloadController,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
    private val sweepOrphanedRecordings: SweepOrphanedRecordingsUseCase,
) : BaseViewModel() {

    val phase: StateFlow<IosRootPhase> = combine(
        observeUserPreferences(),
        appLockManager.lockState,
    ) { preferences, lockState ->
        when {
            !preferences.onboardingCompleted -> IosRootPhase.ONBOARDING
            lockState == AppLockState.LOCKED -> IosRootPhase.LOCKED
            else -> IosRootPhase.READY
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, IosRootPhase.LOADING)

    val isDeviceSecure: Boolean
        get() = appLockManager.isDeviceSecure

    init {
        skipLockForFirstRun(observeUserPreferences)
        finishStartupWhenReady()
        resumeModelDownloadWhenReady()
    }

    fun onUnlockAuthenticated() {
        appLockManager.markUnlocked()
    }

    fun onReady() {
        launchSafely(showLoading = false) {
            if (phase.value != IosRootPhase.READY) return@launchSafely
            resumeInterruptedNotes()
        }
    }

    private fun finishStartupWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            resumeInterruptedNotes()
            sweepOrphanedRecordings()
        }
    }

    // READY implies onboarding is complete, so the user has already agreed to the download.
    private fun resumeModelDownloadWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            if (!modelManager.isModelDownloaded()) {
                modelDownloadController.start()
            }
        }
    }

    private fun skipLockForFirstRun(observeUserPreferences: ObserveUserPreferencesUseCase) {
        launchSafely(showLoading = false) {
            if (!observeUserPreferences().first().onboardingCompleted) {
                appLockManager.markUnlocked()
            }
        }
    }
}
