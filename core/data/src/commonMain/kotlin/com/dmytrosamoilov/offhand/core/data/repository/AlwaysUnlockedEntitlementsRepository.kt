package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.Entitlements
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class AlwaysUnlockedEntitlementsRepository : EntitlementsRepository {

    override fun observeEntitlements(): Flow<Entitlements> =
        flowOf(Entitlements(customStylesUnlocked = true))
}
