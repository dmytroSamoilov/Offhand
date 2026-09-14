package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class ConsentGatedAnalyticsTrackerTest {

    private val logged = mutableListOf<String>()
    private val sink = object : AnalyticsSink {
        override fun log(name: String, params: Map<String, Any>) {
            logged += name
        }
    }
    private val preferences = ConsentPreferences()

    @Test
    fun `events are dropped until consent is granted`() = runTest {
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("note_played"))
        preferences.consent.value = true
        runCurrent()
        tracker.track(AnalyticsEvent("note_paused"))

        assertEquals(listOf("note_paused"), logged)
    }

    @Test
    fun `withdrawn consent stops events again`() = runTest {
        preferences.consent.value = true
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("share_clicked"))
        preferences.consent.value = false
        runCurrent()
        tracker.track(AnalyticsEvent("note_deleted"))

        assertEquals(listOf("share_clicked"), logged)
    }
}

private class ConsentPreferences : UserPreferencesRepository {
    val consent = MutableStateFlow(false)

    override val preferences: Flow<UserPreferences> = consent.map { granted ->
        UserPreferences(
            onboardingCompleted = true,
            appLockEnabled = false,
            telemetryConsent = granted,
            dynamicColor = false,
            developerOptions = false,
            savedRecordingsCount = 0,
            reviewPrompt = ReviewPromptState(),
            noteStyle = NoteStyleRef.DEFAULT,
            proOverride = ProOverride.STORE,
            smartSuggestionsEnabled = false,
        )
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun setAppLockEnabled(enabled: Boolean) = Unit
    override suspend fun setTelemetryConsent(granted: Boolean) = Unit
    override suspend fun setDynamicColor(enabled: Boolean) = Unit
    override suspend fun setDeveloperOptions(enabled: Boolean) = Unit
    override suspend fun setNoteStyle(style: NoteStyleRef) = Unit
    override suspend fun incrementSavedRecordingsCount() = Unit
    override suspend fun setReviewPromptState(state: ReviewPromptState) = Unit
    override suspend fun setProOverride(override: ProOverride) = Unit
    override suspend fun setSmartSuggestionsEnabled(enabled: Boolean) = Unit
}
