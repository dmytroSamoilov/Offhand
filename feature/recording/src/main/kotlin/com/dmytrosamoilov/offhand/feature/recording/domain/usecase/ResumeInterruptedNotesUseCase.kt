package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ResumeInterruptedNotesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notesRepository: NotesRepository,
    private val sessionManager: RecordingSessionManager,
    private val failNote: FailNoteUseCase,
) {
    private val hasRun = AtomicBoolean(false)

    suspend operator fun invoke() {
        if (!hasRun.compareAndSet(false, true)) return
        val activeNoteIds = sessionManager.processingNoteIds.value
        notesRepository.observeNotes().first()
            .filter { it.status == NoteStatus.PROCESSING && it.id !in activeNoteIds }
            .forEach { note ->
                val audioFileName = note.audioFileName
                if (audioFileName == null) {
                    failNote(note.id)
                } else {
                    RecordingService.retryNote(context, note.id, audioFileName)
                }
            }
    }
}
