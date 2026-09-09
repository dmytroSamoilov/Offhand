package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraft
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDrafter

class DraftNoteStyleUseCase(
    private val drafter: NoteStyleDrafter,
) {
    suspend operator fun invoke(description: String): NoteStyleDraft? = drafter.draft(description)
}
