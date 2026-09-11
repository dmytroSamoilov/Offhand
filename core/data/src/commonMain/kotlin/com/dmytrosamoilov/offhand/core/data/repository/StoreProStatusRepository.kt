package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusCache
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// The store's answer wins whenever it has one; until then the last answer
// it gave is trusted, so an offline launch keeps its plan.
internal class StoreProStatusRepository(
    store: ProStore,
    private val cache: ProStatusCache,
) : ProStatusRepository {

    private val status: Flow<ProStatus> = store.status
        .onEach { status -> if (status != null) cache.write(status) }
        .map { status -> status ?: cache.read() }
        .distinctUntilChanged()

    override fun observeStatus(): Flow<ProStatus> = status
}
