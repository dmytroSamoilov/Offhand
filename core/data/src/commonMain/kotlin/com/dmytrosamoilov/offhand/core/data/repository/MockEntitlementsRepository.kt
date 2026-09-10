package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.Entitlements
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Stand-in until Play Billing and StoreKit report real purchases; flip the
// mock here to exercise the locked state.
internal class MockEntitlementsRepository : EntitlementsRepository {

    override fun observeEntitlements(): Flow<Entitlements> = flowOf(MOCK_ENTITLEMENTS)

    private companion object {
        val MOCK_ENTITLEMENTS = Entitlements(
            customStylesUnlocked = true,
            audioImportUnlocked = true,
            calendarSuggestionsUnlocked = true,
        )
    }
}
