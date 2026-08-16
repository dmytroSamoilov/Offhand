package com.dmytrosamoilov.offhand.shared

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class IosRootViewModel(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
) : BaseViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean?> = observeUserPreferences()
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onReady() {
        launchSafely(showLoading = false) {
            resumeInterruptedNotes()
        }
    }
}
