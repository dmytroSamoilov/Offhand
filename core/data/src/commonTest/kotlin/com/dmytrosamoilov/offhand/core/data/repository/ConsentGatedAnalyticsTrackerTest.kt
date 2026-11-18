package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.NoteFeature
import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.TelemetryConsent
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.common.BuildInfo
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
    private val loggedParams = mutableListOf<Map<String, Any>>()
    private val sink = object : AnalyticsSink {
        override fun log(name: String, params: Map<String, Any>) {
            logged += name
            loggedParams += params
        }
    }
    private val preferences = ConsentPreferences()
    private val buildInfo = BuildInfo(isDeveloperBuild = false, platform = "android")

    @Test
    fun `a declined consent drops events and a later share only sends what follows`() = runTest {
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("note_played"))
        preferences.consent.value = TelemetryConsent.SHARED
        runCurrent()
        tracker.track(AnalyticsEvent("note_paused"))

        assertEquals(listOf("note_paused"), logged)
    }

    @Test
    fun `withdrawn consent stops events again`() = runTest {
        preferences.consent.value = TelemetryConsent.SHARED
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("share_clicked"))
        preferences.consent.value = TelemetryConsent.DECLINED
        runCurrent()
        tracker.track(AnalyticsEvent("note_deleted"))

        assertEquals(listOf("share_clicked"), logged)
    }

    @Test
    fun `nothing is sent while the question is unanswered`() = runTest {
        preferences.consent.value = TelemetryConsent.UNANSWERED
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("device_transcripts_only"))
        runCurrent()

        assertEquals(emptyList(), logged)
    }

    @Test
    fun `events from before the answer go out once the answer is share`() = runTest {
        preferences.consent.value = TelemetryConsent.UNANSWERED
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("onboarding_step"))
        preferences.consent.value = TelemetryConsent.SHARED
        runCurrent()

        assertEquals(listOf("onboarding_step"), logged)
    }

    @Test
    fun `events from before the answer are dropped when the answer is do not share`() = runTest {
        preferences.consent.value = TelemetryConsent.UNANSWERED
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("device_unsupported"))
        preferences.consent.value = TelemetryConsent.DECLINED
        runCurrent()
        preferences.consent.value = TelemetryConsent.SHARED
        runCurrent()

        assertEquals(emptyList(), logged)
    }

    @Test
    fun `events tracked before the stored answer has loaded are not lost`() = runTest {
        preferences.consent.value = TelemetryConsent.SHARED
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)

        tracker.track(AnalyticsEvent("note_played"))
        runCurrent()

        assertEquals(listOf("note_played"), logged)
    }

    @Test
    fun `a developer build sends nothing even with consent`() = runTest {
        preferences.consent.value = TelemetryConsent.SHARED
        val developerBuild = BuildInfo(isDeveloperBuild = true, platform = "android")
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, developerBuild, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("note_played"))

        assertEquals(emptyList(), logged)
    }

    @Test
    fun `every event carries the platform next to its own parameters`() = runTest {
        preferences.consent.value = TelemetryConsent.SHARED
        val tracker = ConsentGatedAnalyticsTracker(sink, preferences, buildInfo, backgroundScope)
        runCurrent()

        tracker.track(AnalyticsEvent("note_recorded", mapOf("duration_s" to 12L)))

        assertEquals(listOf(mapOf<String, Any>("duration_s" to 12L, "platform" to "android")), loggedParams)
    }
}

private class ConsentPreferences : UserPreferencesRepository {
    val consent = MutableStateFlow(TelemetryConsent.DECLINED)

    override val preferences: Flow<UserPreferences> = consent.map { answer ->
        UserPreferences(
            onboardingCompleted = true,
            appLockEnabled = false,
            telemetryConsent = answer,
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
    override suspend fun setSkipSilenceEnabled(enabled: Boolean) = Unit
    override suspend fun setNoteFeatureEnabled(feature: NoteFeature, enabled: Boolean) = Unit
    override suspend fun setNotificationsPrompted() = Unit

    override suspend fun setEarlyAdopterPro(decision: EarlyAdopterPro) = Unit

    override suspend fun setEarlyAdopterThanked() = Unit

    override suspend fun incrementFreeImportsUsed() = Unit

    override suspend fun refundFreeImport() = Unit

    override suspend fun setModelDownloadProgress(percent: Int?) = Unit
}
