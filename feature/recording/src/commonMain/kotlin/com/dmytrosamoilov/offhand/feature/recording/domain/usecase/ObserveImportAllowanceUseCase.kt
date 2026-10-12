package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.FreeImportLimits
import com.dmytrosamoilov.offhand.feature.recording.domain.ImportAllowance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveImportAllowanceUseCase(
    private val userPreferences: UserPreferencesRepository,
    private val proStatusRepository: ProStatusRepository,
) {
    operator fun invoke(): Flow<ImportAllowance> =
        combine(proStatusRepository.observeStatus(), userPreferences.preferences) { status, preferences ->
            if (status.isPro) {
                ImportAllowance.Unlimited
            } else {
                ImportAllowance.Free((FreeImportLimits.FREE_IMPORTS - preferences.freeImportsUsed).coerceAtLeast(0))
            }
        }.distinctUntilChanged()
}
