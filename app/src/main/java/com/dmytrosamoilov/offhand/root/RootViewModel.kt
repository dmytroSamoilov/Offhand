package com.dmytrosamoilov.offhand.root

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class RootViewModel(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val appLockManager: AppLockManager,
    private val passphraseProvider: DatabasePassphraseProvider,
    private val modelManager: ModelManager,
    private val modelDownloadController: ModelDownloadController,
    private val resumeInterruptedNotes: Lazy<ResumeInterruptedNotesUseCase>,
    private val sweepOrphanedRecordings: Lazy<SweepOrphanedRecordingsUseCase>,
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
        }
    }

    // READY implies onboarding is complete, so the user has already agreed to the download.
    private fun resumeModelDownloadWhenReady() {
        launchSafely(showLoading = false) {
            uiState.first { it.phase == RootPhase.READY }
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
