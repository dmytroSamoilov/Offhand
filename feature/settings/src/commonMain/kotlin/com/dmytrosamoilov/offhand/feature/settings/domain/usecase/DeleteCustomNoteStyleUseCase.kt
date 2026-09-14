package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class DeleteCustomNoteStyleUseCase(
    private val repository: CustomNoteStylesRepository,
    private val preferences: UserPreferencesRepository,
) {
    suspend operator fun invoke(id: Long) {
        if (preferences.preferences.first().noteStyle == NoteStyleRef.Custom(id)) {
            preferences.setNoteStyle(NoteStyleRef.DEFAULT)
        }
        repository.deleteStyle(id)
    }
}
