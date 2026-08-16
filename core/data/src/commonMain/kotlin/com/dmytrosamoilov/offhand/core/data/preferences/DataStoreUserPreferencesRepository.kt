package com.dmytrosamoilov.offhand.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val buildInfo: BuildInfo,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> =
        dataStore.data.map { preferences ->
            UserPreferences(
                onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false,
                telemetryConsent = preferences[KEY_TELEMETRY_CONSENT] ?: false,
                dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: false,
                developerOptions = buildInfo.isDebugBuild &&
                    (preferences[KEY_DEVELOPER_OPTIONS] ?: false),
                savedRecordingsCount = preferences[KEY_SAVED_RECORDINGS_COUNT] ?: 0,
                reviewPrompt = ReviewPromptState(
                    burstStartedAtMs = preferences[KEY_REVIEW_BURST_STARTED_AT_MS] ?: 0L,
                    attemptCount = preferences[KEY_REVIEW_BURST_ATTEMPTS] ?: 0,
                    lastAttemptAtMs = preferences[KEY_LAST_REVIEW_ATTEMPT_AT_MS]
                        ?: preferences[KEY_LEGACY_LAST_REVIEW_REQUEST_AT_MS] ?: 0L,
                ),
                notePreset = NotePreset.fromName(preferences[KEY_NOTE_PRESET]),
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setTelemetryConsent(granted: Boolean) {
        dataStore.edit { it[KEY_TELEMETRY_CONSENT] = granted }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setDeveloperOptions(enabled: Boolean) {
        dataStore.edit { it[KEY_DEVELOPER_OPTIONS] = enabled }
    }

    override suspend fun setNotePreset(preset: NotePreset) {
        dataStore.edit { it[KEY_NOTE_PRESET] = preset.name }
    }

    override suspend fun incrementSavedRecordingsCount() {
        dataStore.edit { preferences ->
            preferences[KEY_SAVED_RECORDINGS_COUNT] =
                (preferences[KEY_SAVED_RECORDINGS_COUNT] ?: 0) + 1
        }
    }

    override suspend fun setReviewPromptState(state: ReviewPromptState) {
        dataStore.edit { preferences ->
            preferences[KEY_REVIEW_BURST_STARTED_AT_MS] = state.burstStartedAtMs
            preferences[KEY_REVIEW_BURST_ATTEMPTS] = state.attemptCount
            preferences[KEY_LAST_REVIEW_ATTEMPT_AT_MS] = state.lastAttemptAtMs
        }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_TELEMETRY_CONSENT = booleanPreferencesKey("telemetry_consent")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_DEVELOPER_OPTIONS = booleanPreferencesKey("developer_options")
        val KEY_SAVED_RECORDINGS_COUNT = intPreferencesKey("saved_recordings_count")
        val KEY_REVIEW_BURST_STARTED_AT_MS = longPreferencesKey("review_burst_started_at_ms")
        val KEY_REVIEW_BURST_ATTEMPTS = intPreferencesKey("review_burst_attempts")
        val KEY_LAST_REVIEW_ATTEMPT_AT_MS = longPreferencesKey("last_review_attempt_at_ms")
        val KEY_LEGACY_LAST_REVIEW_REQUEST_AT_MS = longPreferencesKey("last_review_request_at_ms")
        val KEY_NOTE_PRESET = stringPreferencesKey("note_preset")
    }
}
