package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import kotlinx.coroutines.flow.first

class SweepOrphanedRecordingsUseCase(
    private val notesRepository: NotesRepository,
    private val audioStore: EncryptedAudioStore,
) {
    suspend operator fun invoke() {
        val referenced = notesRepository.observeNotes().first()
            .mapNotNull { it.audioFileName }
            .toSet()
        val deleted = audioStore.deleteUnreferenced(referenced, MIN_ORPHAN_AGE_MS)
        if (deleted > 0) {
            Logger.withTag(LOG_TAG).i { "Deleted $deleted orphaned recording(s)" }
        }
    }

    private companion object {
        const val LOG_TAG = "RecordingSession"
        const val MIN_ORPHAN_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
