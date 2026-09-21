package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleErrors
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidation
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidator
import kotlinx.coroutines.flow.first

sealed interface SaveNoteStyleResult {
    data class Saved(val id: Long) : SaveNoteStyleResult
    data class Invalid(val errors: NoteStyleErrors) : SaveNoteStyleResult
}

class SaveCustomNoteStyleUseCase(
    private val repository: CustomNoteStylesRepository,
) {
    suspend operator fun invoke(style: CustomNoteStyle): SaveNoteStyleResult {
        val existing = repository.observeStyles().first()
        return when (val validation = NoteStyleValidator.validate(style, existing)) {
            is NoteStyleValidation.Invalid -> SaveNoteStyleResult.Invalid(validation.errors)
            is NoteStyleValidation.Valid -> SaveNoteStyleResult.Saved(persist(validation.style))
        }
    }

    private suspend fun persist(style: CustomNoteStyle): Long {
        if (style.id == 0L) return repository.createStyle(style)
        repository.updateStyle(style)
        return style.id
    }
}
