package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// The store's answer wins whenever it has one; until then the last answer
// it gave is trusted, so an offline launch keeps its plan. An early-adopter
// grant stands in when the store reports nothing, and steps aside for the
// FREE debug override so the free path stays walkable on a dev install.
internal class StoreProStatusRepository(
    store: ProStore,
    private val cache: ProStatusCache,
    preferences: UserPreferencesRepository,
) : ProStatusRepository {

    private val storeStatus: Flow<ProStatus> = store.status
        .onEach { status -> if (status != null) cache.write(status) }
        .map { status -> status ?: cache.read() }

    private val status: Flow<ProStatus> = combine(storeStatus, preferences.preferences) { status, prefs ->
        if (status.isPro || !prefs.isEarlyAdopterProActive()) status else ProStatus.INCLUDED
    }.distinctUntilChanged()

    override fun observeStatus(): Flow<ProStatus> = status
}

private fun UserPreferences.isEarlyAdopterProActive(): Boolean =
    earlyAdopterPro == EarlyAdopterPro.GRANTED && proOverride != ProOverride.FREE
