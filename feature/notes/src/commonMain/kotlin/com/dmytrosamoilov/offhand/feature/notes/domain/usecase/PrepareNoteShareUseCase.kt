package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareBundle

interface PrepareNoteShareUseCase {

    suspend operator fun invoke(
        note: Note,
        includeNote: Boolean,
        includeAudio: Boolean,
    ): NoteShareBundle
}
