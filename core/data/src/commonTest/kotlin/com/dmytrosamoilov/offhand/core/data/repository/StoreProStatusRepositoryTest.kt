package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
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

class StoreProStatusRepositoryTest {

    private val store = StubProStore()
    private val cache = MemoryProStatusCache()
    private val preferences = GrantPreferences()
    private val repository = StoreProStatusRepository(store, cache, preferences)

    @Test
    fun `the store answer is cached and wins`() = runTest {
        store.status.value = ProStatus.LIFETIME

        assertEquals(ProStatus.LIFETIME, repository.observeStatus().first())
        assertEquals(ProStatus.LIFETIME, cache.read())
    }

    @Test
    fun `without a store answer the cached status is used`() = runTest {
        cache.write(ProStatus(plan = ProPlan.YEARLY))
        store.status.value = null

        assertEquals(ProPlan.YEARLY, repository.observeStatus().first().plan)
    }

    @Test
    fun `an early adopter grant stands in when the store reports no purchase`() = runTest {
        preferences.grant.value = EarlyAdopterPro.GRANTED

        assertEquals(ProStatus.INCLUDED, repository.observeStatus().first())
        assertEquals(ProStatus.FREE, cache.read())
    }

    @Test
    fun `a purchase outranks the grant and the free override hides it`() = runTest {
        preferences.grant.value = EarlyAdopterPro.GRANTED
        store.status.value = ProStatus.LIFETIME
        assertEquals(ProStatus.LIFETIME, repository.observeStatus().first())

        store.status.value = ProStatus.FREE
        preferences.override.value = ProOverride.FREE
        assertEquals(ProStatus.FREE, repository.observeStatus().first())
    }
}

private class StubProStore : ProStore {
    override val status = MutableStateFlow<ProStatus?>(ProStatus.FREE)
    override suspend fun loadOffers(): List<ProOffer> = emptyList()
    override suspend fun purchase(plan: ProPlan): PurchaseOutcome = PurchaseOutcome.CANCELLED
    override suspend fun restore(): ProStatus? = status.value
    override fun refresh() = Unit
}

private class MemoryProStatusCache : ProStatusCache {
    private var stored = ProStatus.FREE
    override suspend fun read(): ProStatus = stored
    override suspend fun write(status: ProStatus) {
        stored = status
    }
}

private class GrantPreferences : UserPreferencesRepository {
    val grant = MutableStateFlow(EarlyAdopterPro.UNDECIDED)
    val override = MutableStateFlow(ProOverride.STORE)

    override val preferences: Flow<UserPreferences> = grant.map { current ->
        UserPreferences(
            onboardingCompleted = true,
            appLockEnabled = false,
            telemetryConsent = false,
            dynamicColor = false,
            developerOptions = false,
            savedRecordingsCount = 0,
            reviewPrompt = ReviewPromptState(),
            noteStyle = NoteStyleRef.DEFAULT,
            proOverride = override.value,
            smartSuggestionsEnabled = false,
            earlyAdopterPro = current,
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
    override suspend fun setNotificationsPrompted() = Unit
    override suspend fun setProOverride(override: ProOverride) = Unit

    override suspend fun setEarlyAdopterPro(decision: EarlyAdopterPro) {
        grant.value = decision
    }

    override suspend fun setEarlyAdopterThanked() = Unit

    override suspend fun incrementFreeImportsUsed() = Unit

    override suspend fun refundFreeImport() = Unit

    override suspend fun setModelDownloadProgress(percent: Int?) = Unit
}
