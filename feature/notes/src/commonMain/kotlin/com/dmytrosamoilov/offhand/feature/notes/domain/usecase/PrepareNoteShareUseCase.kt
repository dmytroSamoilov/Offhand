package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareBundle
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteExportFormat

interface PrepareNoteShareUseCase {

    suspend operator fun invoke(
        note: Note,
        noteFormat: NoteExportFormat?,
        includeAudio: Boolean,
    ): NoteShareBundle
}
