package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// The default new notes really get: without Pro that is Summary, and the
// stored choice comes back with the plan.
class ObserveNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
    private val entitlements: ProStatusRepository,
) {
    operator fun invoke(): Flow<NoteStyleRef> =
        combine(repository.preferences, entitlements.observeStatus()) { preferences, status ->
            if (status.isPro) preferences.noteStyle else NoteStyleRef.DEFAULT
        }.distinctUntilChanged()
}
