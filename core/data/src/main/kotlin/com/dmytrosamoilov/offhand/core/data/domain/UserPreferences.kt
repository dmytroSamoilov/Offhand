package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val telemetryConsent: Boolean,
    val dynamicColor: Boolean,
    val developerOptions: Boolean,
    val savedRecordingsCount: Int,
    val reviewPrompt: ReviewPromptState,
    val notePreset: NotePreset,
)

data class ReviewPromptState(
    val burstStartedAtMs: Long = 0L,
    val attemptCount: Int = 0,
    val lastAttemptAtMs: Long = 0L,
)

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setTelemetryConsent(granted: Boolean)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setDeveloperOptions(enabled: Boolean)

    suspend fun setNotePreset(preset: NotePreset)

    suspend fun incrementSavedRecordingsCount()

    suspend fun setReviewPromptState(state: ReviewPromptState)
}
