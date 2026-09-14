package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class MoveNoteToFolderUseCase(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(noteId: Long, folderId: Long?) = repository.moveNoteToFolder(noteId, folderId)
}
