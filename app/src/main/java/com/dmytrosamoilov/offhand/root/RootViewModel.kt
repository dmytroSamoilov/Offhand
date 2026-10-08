package com.dmytrosamoilov.offhand.root

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class RootViewModel(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val appLockManager: AppLockManager,
    private val passphraseProvider: DatabasePassphraseProvider,
    private val modelManager: ModelManager,
    private val modelDownloadLauncher: ModelDownloadLauncher,
    private val resumeInterruptedNotes: Lazy<ResumeInterruptedNotesUseCase>,
    private val sweepOrphanedRecordings: Lazy<SweepOrphanedRecordingsUseCase>,
    private val clearShareCache: ClearShareCacheUseCase,
    private val appForegroundState: AppForegroundState,
) : BaseViewModel() {

    val uiState: StateFlow<RootUiState> = combine(
        observeUserPreferences(),
        appLockManager.lockState,
    ) { preferences, lockState ->
        RootUiState(
            phase = when {
                !preferences.onboardingCompleted -> RootPhase.ONBOARDING
                preferences.appLockEnabled && lockState == AppLockState.LOCKED -> RootPhase.LOCKED
                else -> RootPhase.READY
            },
            isDynamicColorEnabled = preferences.dynamicColor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RootUiState(),
    )

    init {
        skipLockForFirstRun(observeUserPreferences)
        resumeInterruptedNotesWhenReady()
        resumeInterruptedNotesOnReturn()
        resumeModelDownloadWhenReady()
    }

    fun onUnlockAuthenticated() {
        appLockManager.markUnlocked()
        launchSafely(showLoading = false) {
            withContext(Dispatchers.IO) {
                passphraseProvider.warmUp()
            }
        }
    }

    // Lazy because the use case transitively opens the encrypted database — pre-0.9.1
    // installs still hold an auth-bound Keystore key that throws
    // UserNotAuthenticatedException until their first unlock migrates it.
    // Only touch it once the app is READY.
    private fun resumeInterruptedNotesWhenReady() {
        launchSafely(showLoading = false) {
            uiState.first { it.phase == RootPhase.READY }
            resumeInterruptedNotes.value.invoke()
            sweepOrphanedRecordings.value.invoke()
            clearShareCache()
        }
    }

    // A note the system paused while the app was away continues on the way back
    // in, without waiting for the next process start.
    private fun resumeInterruptedNotesOnReturn() {
        launchSafely(showLoading = false) {
            uiState.first { it.phase == RootPhase.READY }
            appForegroundState.isInForeground.drop(1).filter { it }.collect {
                resumeInterruptedNotes.value.invoke()
            }
        }
    }

    // READY implies onboarding is complete, so the user has already agreed to the download.
    private fun resumeModelDownloadWhenReady() {
        launchSafely(showLoading = false) {
            uiState.first { it.phase == RootPhase.READY }
            if (!modelManager.isModelDownloaded()) {
                modelDownloadLauncher.startWhenAllowed()
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
