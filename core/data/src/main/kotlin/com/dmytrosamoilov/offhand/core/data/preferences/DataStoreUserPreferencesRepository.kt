package com.dmytrosamoilov.offhand.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

@Singleton
internal class DataStoreUserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildInfo: BuildInfo,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> =
        context.userPreferencesDataStore.data.map { preferences ->
            UserPreferences(
                onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false,
                telemetryConsent = preferences[KEY_TELEMETRY_CONSENT] ?: false,
                dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: false,
                developerOptions = buildInfo.isDebugBuild &&
                    (preferences[KEY_DEVELOPER_OPTIONS] ?: false),
                savedRecordingsCount = preferences[KEY_SAVED_RECORDINGS_COUNT] ?: 0,
                lastReviewRequestAtMs = preferences[KEY_LAST_REVIEW_REQUEST_AT_MS] ?: 0L,
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setTelemetryConsent(granted: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_TELEMETRY_CONSENT] = granted }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setDeveloperOptions(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_DEVELOPER_OPTIONS] = enabled }
    }

    override suspend fun incrementSavedRecordingsCount() {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_SAVED_RECORDINGS_COUNT] =
                (preferences[KEY_SAVED_RECORDINGS_COUNT] ?: 0) + 1
        }
    }

    override suspend fun setLastReviewRequestAt(timestampMs: Long) {
        context.userPreferencesDataStore.edit { it[KEY_LAST_REVIEW_REQUEST_AT_MS] = timestampMs }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_TELEMETRY_CONSENT = booleanPreferencesKey("telemetry_consent")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_DEVELOPER_OPTIONS = booleanPreferencesKey("developer_options")
        val KEY_SAVED_RECORDINGS_COUNT = intPreferencesKey("saved_recordings_count")
        val KEY_LAST_REVIEW_REQUEST_AT_MS = longPreferencesKey("last_review_request_at_ms")
    }
}
