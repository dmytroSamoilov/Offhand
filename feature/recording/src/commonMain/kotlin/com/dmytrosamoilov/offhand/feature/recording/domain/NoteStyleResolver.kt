package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

class NoteStyleResolver(
    private val customNoteStylesRepository: CustomNoteStylesRepository,
) {

    internal suspend fun resolve(style: NoteStyleRef): NoteStyleSpec = when (style) {
        is NoteStyleRef.BuiltIn -> BuiltInNoteStyles.spec(style.preset)
        is NoteStyleRef.Custom -> customNoteStylesRepository.getStyle(style.id)
            ?.let(CustomNoteStyleSpecBuilder::build)
            ?: BuiltInNoteStyles.spec(NotePreset.DEFAULT)
    }
}
