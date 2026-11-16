package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// The store's answer wins whenever it has one; until then the last answer
// it gave is trusted, so an offline launch keeps its plan. An early-adopter
// grant stands in when the store reports nothing, then the fourteen-day
// trial while it runs; both step aside for the FREE debug override so the
// free path stays walkable on a dev install.
internal class StoreProStatusRepository(
    store: ProStore,
    private val cache: ProStatusCache,
    preferences: UserPreferencesRepository,
    trial: ProTrialRepository,
) : ProStatusRepository {

    private val storeStatus: Flow<ProStatus> = store.status
        .onEach { status -> if (status != null) cache.write(status) }
        .map { status -> status ?: cache.read() }

    private val status: Flow<ProStatus> =
        combine(storeStatus, preferences.preferences, trial.observeState()) { status, prefs, trialState ->
            when {
                status.isPro || prefs.proOverride == ProOverride.FREE -> status
                prefs.earlyAdopterPro == EarlyAdopterPro.GRANTED -> ProStatus.INCLUDED
                trialState is ProTrialState.Active -> ProStatus.trial(trialState.endsAtMs)
                else -> status
            }
        }.distinctUntilChanged()

    override fun observeStatus(): Flow<ProStatus> = status
}
