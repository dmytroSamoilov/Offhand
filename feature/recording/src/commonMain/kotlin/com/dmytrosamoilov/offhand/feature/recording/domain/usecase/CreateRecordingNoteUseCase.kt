@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first

class CreateRecordingNoteUseCase(
    private val defaultNoteTitleProvider: DefaultNoteTitleProvider,
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
) {
    suspend operator fun invoke(
        audioFileName: String?,
        style: NoteStyleRef,
        folderId: Long?,
    ): Long =
        notesRepository.createNote(
            Note(
                id = 0,
                title = defaultNoteTitleProvider.titleFor(notesRepository.countNotes() + 1),
                body = "",
                transcript = "",
                createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                transcriptionTimeMs = null,
                structuringTimeMs = null,
                hardwareBackend = null,
                audioFileName = audioFileName,
                durationMs = null,
                status = NoteStatus.RECORDING,
                style = style,
                folderId = folderId?.takeIf { isExistingFolder(it) },
            ),
        )

    // A widget or a shortcut can still name a folder that was deleted since;
    // a note filed there would show up nowhere.
    private suspend fun isExistingFolder(folderId: Long): Boolean =
        foldersRepository.observeFolders().first().any { it.id == folderId }
}
