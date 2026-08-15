package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import timber.log.Timber

class SweepOrphanedRecordingsUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
    private val audioStore: EncryptedAudioStore,
) {
    suspend operator fun invoke() {
        val referenced = notesRepository.observeNotes().first()
            .mapNotNull { it.audioFileName }
            .toSet()
        val deleted = audioStore.deleteUnreferenced(referenced, MIN_ORPHAN_AGE_MS)
        if (deleted > 0) {
            Timber.tag(LOG_TAG).i("Deleted %d orphaned recording(s)", deleted)
        }
    }

    private companion object {
        const val LOG_TAG = "RecordingSession"
        const val MIN_ORPHAN_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
