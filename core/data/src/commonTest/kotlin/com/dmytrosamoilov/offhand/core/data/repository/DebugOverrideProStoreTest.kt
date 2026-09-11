package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugOverrideProStoreTest {

    private val platform = RecordingProStore()
    private val preferences = FakePreferences()
    private val store = DebugOverrideProStore(platform, preferences)

    @Test
    fun `without an override the platform store answers everything`() = runTest {
        assertEquals(ProStatus.FREE, store.status.first())
        assertEquals(PurchaseOutcome.CANCELLED, store.purchase(ProPlan.YEARLY))
        assertEquals(listOf(ProPlan.YEARLY), platform.purchased)
    }

    @Test
    fun `a free override hides the platform purchase and a purchase flips it to pro`() = runTest {
        platform.platformStatus.value = ProStatus.LIFETIME
        preferences.override.value = ProOverride.FREE

        assertEquals(ProStatus.FREE, store.status.first())
        assertEquals(2, store.loadOffers().size)
        assertEquals(PurchaseOutcome.PURCHASED, store.purchase(ProPlan.LIFETIME))
        assertEquals(ProOverride.PRO, preferences.override.value)
        assertEquals(ProStatus.LIFETIME, store.status.first())
        assertEquals(emptyList(), platform.purchased)
    }

    @Test
    fun `restore answers from the override without touching the store`() = runTest {
        preferences.override.value = ProOverride.PRO

        assertEquals(ProStatus.LIFETIME, store.restore())
        assertEquals(0, platform.restores)
    }
}

private class RecordingProStore : ProStore {
    val platformStatus = MutableStateFlow<ProStatus?>(ProStatus.FREE)
    override val status: Flow<ProStatus?> = platformStatus
    val purchased = mutableListOf<ProPlan>()
    var restores = 0

    override suspend fun loadOffers(): List<ProOffer> = emptyList()

    override suspend fun purchase(plan: ProPlan): PurchaseOutcome {
        purchased += plan
        return PurchaseOutcome.CANCELLED
    }

    override suspend fun restore(): ProStatus? {
        restores += 1
        return platformStatus.value
    }

    override fun refresh() = Unit
}

private class FakePreferences : UserPreferencesRepository {
    val override = MutableStateFlow(ProOverride.STORE)

    override val preferences: Flow<UserPreferences> = override.map { current ->
        UserPreferences(
            onboardingCompleted = true,
            appLockEnabled = false,
            telemetryConsent = false,
            dynamicColor = false,
            developerOptions = false,
            savedRecordingsCount = 0,
            reviewPrompt = ReviewPromptState(),
            noteStyle = NoteStyleRef.DEFAULT,
            proOverride = current,
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
    override suspend fun setSmartSuggestionsEnabled(enabled: Boolean) = Unit

    override suspend fun setProOverride(override: ProOverride) {
        this.override.value = override
    }
}
