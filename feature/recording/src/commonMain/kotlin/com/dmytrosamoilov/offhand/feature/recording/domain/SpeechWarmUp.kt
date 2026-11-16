package com.dmytrosamoilov.offhand.feature.recording.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteFeaturesUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Keeps the speech engine in memory while the app is in front, so the first
// words of a recording are transcribed without waiting for it to load, and
// gives the memory back once the app is away. It only ever acts while nothing
// is being recorded or processed: a recording that goes on in the background
// keeps its engine, and the pipeline loads and frees the engine itself while
// it works.
class SpeechWarmUp(
    private val speechToText: SpeechToText,
    private val sessionManager: RecordingSessionManager,
    private val appForegroundState: AppForegroundState,
    private val getNoteFeatures: GetNoteFeaturesUseCase,
) {

    private val mutex = Mutex()

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(appForegroundState.isInForeground, isBusy(), speechToText.downloadState) { inForeground, busy, model ->
                Situation(inForeground, busy, model is SpeechModelState.Downloaded)
            }.distinctUntilChanged().collect { situation -> if (!situation.isBusy) settle() }
        }
    }

    private fun isBusy(): Flow<Boolean> = combine(
        sessionManager.session,
        sessionManager.processingNoteIds,
        sessionManager.transcribingNoteIds,
    ) { session, processing, transcribing ->
        session.phase != SessionPhase.IDLE || processing.isNotEmpty() || transcribing.isNotEmpty()
    }

    // Checked again under the lock: a recording may have started between the
    // change that brought us here and now.
    private suspend fun settle() = mutex.withLock {
        if (isBusyNow()) return@withLock
        if (appForegroundState.isInForeground.value) warmUp() else speechToText.release()
    }

    private fun isBusyNow(): Boolean =
        sessionManager.session.value.phase != SessionPhase.IDLE ||
            sessionManager.processingNoteIds.value.isNotEmpty() ||
            sessionManager.transcribingNoteIds.value.isNotEmpty()

    // Never a reason to download anything: without the files on disk, or with
    // transcription switched off, there is nothing to warm up.
    private suspend fun warmUp() {
        if (speechToText.downloadState.value !is SpeechModelState.Downloaded) return
        if (!getNoteFeatures().isTranscriptionEnabled) return
        try {
            speechToText.prepare()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Speech engine warm-up failed, it loads with the next recording" }
        }
    }

    private data class Situation(
        val isInForeground: Boolean,
        val isBusy: Boolean,
        val isModelOnDisk: Boolean,
    )

    private companion object {
        const val LOG_TAG = "SpeechWarmUp"
    }
}
