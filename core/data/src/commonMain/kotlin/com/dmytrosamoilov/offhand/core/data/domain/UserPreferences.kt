package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val appLockEnabled: Boolean,
    val telemetryConsent: Boolean,
    val dynamicColor: Boolean,
    val developerOptions: Boolean,
    val savedRecordingsCount: Int,
    val reviewPrompt: ReviewPromptState,
    val noteStyle: NoteStyleRef,
    val proOverride: ProOverride,
    val smartSuggestionsEnabled: Boolean,
    val notificationsPrompted: Boolean = false,
    val earlyAdopterPro: EarlyAdopterPro = EarlyAdopterPro.UNDECIDED,
    // The thank-you for the grant stays up until it is dismissed, whatever
    // happens to the process in between.
    val isEarlyAdopterThanked: Boolean = false,
    val freeImportsUsed: Int = 0,
    // Percent of an on-device AI download that has not finished yet; null when
    // none is pending, so a cold start can tell an abandoned download apart.
    val modelDownloadProgress: Int? = null,
)

// Decided once per install when 1.5 first runs: an install that had finished
// onboarding and holds at least one note keeps Pro on this device for good.
enum class EarlyAdopterPro {
    UNDECIDED,
    GRANTED,
    NONE,
}

// Debug builds only: lets a dev install walk the free and the Pro paths
// without a store purchase.
enum class ProOverride {
    STORE,
    FREE,
    PRO,
}

data class ReviewPromptState(
    val burstStartedAtMs: Long = 0L,
    val attemptCount: Int = 0,
    val lastAttemptAtMs: Long = 0L,
)

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setAppLockEnabled(enabled: Boolean)

    suspend fun setTelemetryConsent(granted: Boolean)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setDeveloperOptions(enabled: Boolean)

    suspend fun setNoteStyle(style: NoteStyleRef)

    suspend fun incrementSavedRecordingsCount()

    suspend fun setReviewPromptState(state: ReviewPromptState)

    suspend fun setProOverride(override: ProOverride)

    suspend fun setSmartSuggestionsEnabled(enabled: Boolean)

    suspend fun setNotificationsPrompted()

    suspend fun setEarlyAdopterPro(decision: EarlyAdopterPro)

    suspend fun setEarlyAdopterThanked()

    suspend fun incrementFreeImportsUsed()

    suspend fun refundFreeImport()

    suspend fun setModelDownloadProgress(percent: Int?)
}

val UserPreferences.isEarlyAdopterThanksPending: Boolean
    get() = earlyAdopterPro == EarlyAdopterPro.GRANTED && !isEarlyAdopterThanked
