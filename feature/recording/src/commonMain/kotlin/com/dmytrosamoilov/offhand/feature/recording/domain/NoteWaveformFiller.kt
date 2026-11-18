@file:OptIn(ExperimentalAtomicApi::class)

package com.dmytrosamoilov.offhand.feature.recording.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.data.domain.ProtectedDataState
import com.dmytrosamoilov.offhand.core.data.domain.awaitAvailable
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MeasureNoteWaveformUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ObserveNotesWithoutWaveformUseCase
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

// Measures every recording that has no waveform yet, one at a time and in
// the background: new recordings and imports once their audio is complete,
// and the whole library after the upgrade or a restore. A note that fails is
// left for the next process, so an unreadable file is not retried in a loop.
// Started once the app is READY, because reading the notes opens the database;
// each measurement waits while the device is locked, when the recordings cannot be opened.
class NoteWaveformFiller(
    private val observeNotesWithoutWaveform: ObserveNotesWithoutWaveformUseCase,
    private val measureNoteWaveform: MeasureNoteWaveformUseCase,
    private val protectedData: ProtectedDataState,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isStarted = AtomicBoolean(false)
    private val attemptedNoteIds = HashSet<Long>()

    fun start() {
        if (!isStarted.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch {
            observeNotesWithoutWaveform().conflate().collect { notes ->
                notes.filter { attemptedNoteIds.add(it.id) }.forEach { note ->
                    measure(note.id, note.audioFileName ?: return@forEach)
                }
            }
        }
    }

    private suspend fun measure(noteId: Long, audioFileName: String) {
        protectedData.awaitAvailable()
        try {
            measureNoteWaveform(noteId, audioFileName)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Waveform of note $noteId could not be measured" }
        }
    }

    private companion object {
        const val LOG_TAG = "NoteWaveformFiller"
    }
}
