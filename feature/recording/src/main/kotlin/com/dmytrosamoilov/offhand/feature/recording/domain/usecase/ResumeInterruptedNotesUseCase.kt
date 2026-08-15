package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

@Singleton
class ResumeInterruptedNotesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notesRepository: NotesRepository,
    private val sessionManager: RecordingSessionManager,
    private val failNote: FailNoteUseCase,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val audioStore: EncryptedAudioStore,
) {

    suspend operator fun invoke() {
        if (!isAiCoreDownloaded()) return
        val activeNoteIds = sessionManager.processingNoteIds.value
        val liveRecordingNoteId = sessionManager.activeRecordingNoteId.value
        notesRepository.observeNotes().first()
            .filter { it.status == NoteStatus.PROCESSING || it.status == NoteStatus.RECORDING }
            .filter { it.id !in activeNoteIds && it.id != liveRecordingNoteId }
            .sortedBy { it.createdAtEpochMs }
            .forEach { note -> resume(note) }
    }

    private suspend fun resume(note: Note) {
        val audioFileName = note.audioFileName
        val resumed = backfillDurationIfMissing(note, audioFileName)
        when {
            resumed.transcript.isNotBlank() -> restructureViaService(resumed.id, resumed.preset)
            audioFileName != null -> retryViaService(resumed.id, audioFileName)
            resumed.status == NoteStatus.RECORDING -> notesRepository.deleteNote(resumed.id)
            else -> failNote(resumed.id)
        }
    }

    // A note interrupted mid-recording never went through the drain step that
    // stamps the duration, so it is derived from the decrypted audio size.
    private suspend fun backfillDurationIfMissing(note: Note, audioFileName: String?): Note {
        if (note.durationMs != null || audioFileName == null) return note
        val pcmBytes = runCatching { audioStore.pcmSizeOf(audioFileName) }
            .onFailure { Timber.tag(LOG_TAG).w(it, "Duration backfill failed for note %d", note.id) }
            .getOrNull()
            ?: return note
        if (pcmBytes <= 0) return note
        val updated = note.copy(durationMs = pcmBytes * 1000 / PCM_BYTES_PER_SECOND)
        notesRepository.updateNote(updated)
        return updated
    }

    private fun retryViaService(noteId: Long, audioFileName: String) {
        try {
            RecordingService.retryNote(context, noteId, audioFileName)
        } catch (notAllowed: ForegroundServiceStartNotAllowedException) {
            Timber.tag(LOG_TAG).w(notAllowed, "FGS not allowed, processing note %d in-process", noteId)
            sessionManager.retryNote(noteId, audioFileName)
        }
    }

    private fun restructureViaService(noteId: Long, preset: NotePreset) {
        try {
            RecordingService.restructureNote(context, noteId, preset)
        } catch (notAllowed: ForegroundServiceStartNotAllowedException) {
            Timber.tag(LOG_TAG).w(notAllowed, "FGS not allowed, structuring note %d in-process", noteId)
            sessionManager.restructureNote(noteId, preset)
        }
    }

    private companion object {
        const val LOG_TAG = "RecordingSession"
        const val PCM_BYTES_PER_SECOND = StreamingAudioRecorder.SAMPLE_RATE * 2L
    }
}
