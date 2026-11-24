package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteFeatures
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.noteFeatures
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveNoteFeaturesUseCase(
    private val repository: UserPreferencesRepository,
    private val deviceTier: DeviceTierProvider,
) {
    operator fun invoke(): Flow<NoteFeatures> =
        repository.preferences.map { it.noteFeatures(isSummaryAvailable = deviceTier.writesSummaries()) }.distinctUntilChanged()
}
