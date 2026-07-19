package com.dmytrosamoilov.offhand.root

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import com.dmytrosamoilov.offhand.core.security.DatabasePassphraseProvider
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@HiltViewModel
class RootViewModel @Inject constructor(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val appLockManager: AppLockManager,
    private val passphraseProvider: DatabasePassphraseProvider,
    private val resumeInterruptedNotes: Lazy<ResumeInterruptedNotesUseCase>,
) : BaseViewModel() {

    val uiState: StateFlow<RootUiState> = combine(
        observeUserPreferences(),
        appLockManager.lockState,
    ) { preferences, lockState ->
        RootUiState(
            phase = when {
                !preferences.onboardingCompleted -> RootPhase.ONBOARDING
                lockState == AppLockState.LOCKED -> RootPhase.LOCKED
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
    }

    fun onUnlockAuthenticated() {
        appLockManager.markUnlocked()
        launchSafely(showLoading = false) {
            withContext(Dispatchers.IO) {
                passphraseProvider.warmUp()
            }
        }
    }

    // Lazy because the use case transitively opens the encrypted database —
    // unwrapping its Keystore passphrase before the user authenticates throws
    // UserNotAuthenticatedException. Only touch it once the app is READY.
    private fun resumeInterruptedNotesWhenReady() {
        launchSafely(showLoading = false) {
            uiState.first { it.phase == RootPhase.READY }
            resumeInterruptedNotes.get().invoke()
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
