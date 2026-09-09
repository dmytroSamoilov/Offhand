package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Entitlements
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import kotlinx.coroutines.flow.Flow

class ObserveEntitlementsUseCase(
    private val repository: EntitlementsRepository,
) {
    operator fun invoke(): Flow<Entitlements> = repository.observeEntitlements()
}
