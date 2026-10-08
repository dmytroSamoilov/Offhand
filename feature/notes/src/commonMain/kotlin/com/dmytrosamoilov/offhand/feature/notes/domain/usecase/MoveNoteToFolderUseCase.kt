package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetFolderStyleUseCase

// A note that lands in a folder with its own style is rewritten in that
// style, unless it already has it or is still being written.
class MoveNoteToFolderUseCase(
    private val repository: NotesRepository,
    private val getFolderStyle: GetFolderStyleUseCase,
    private val recordingProcessController: RecordingProcessController,
) {
    suspend operator fun invoke(noteId: Long, folderId: Long?) {
        repository.moveNoteToFolder(noteId, folderId)
        val style = getFolderStyle(folderId) ?: return
        val note = repository.getNote(noteId) ?: return
        if (note.status != NoteStatus.READY || note.style == style || note.transcript.isBlank()) return
        recordingProcessController.restructureNote(noteId, style)
    }
}
