package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val telemetryConsent: Boolean,
    val dynamicColor: Boolean,
    val developerOptions: Boolean,
    val savedRecordingsCount: Int,
    val lastReviewRequestAtMs: Long,
)

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setTelemetryConsent(granted: Boolean)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setDeveloperOptions(enabled: Boolean)

    suspend fun incrementSavedRecordingsCount()

    suspend fun setLastReviewRequestAt(timestampMs: Long)
}
