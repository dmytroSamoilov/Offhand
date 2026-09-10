package com.dmytrosamoilov.offhand.feature.recording.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import timber.log.Timber

class RecordingProcessControllerImpl(
    private val context: Context,
) : RecordingProcessController {

    override fun startRecording() {
        RecordingService.start(context)
    }

    override fun retryNote(noteId: Long, audioFileName: String): Boolean = startServiceCall(noteId) {
        RecordingService.retryNote(context, noteId, audioFileName)
    }

    override fun restructureNote(noteId: Long, style: NoteStyleRef): Boolean = startServiceCall(noteId) {
        RecordingService.restructureNote(context, noteId, style)
    }

    override fun importAudio(source: AudioImportSource): Boolean = startServiceCall(IMPORT_NOTE_ID) {
        RecordingService.importAudio(context, source)
    }

    override fun suggestEvents(noteId: Long): Boolean = startServiceCall(noteId) {
        RecordingService.suggestEvents(context, noteId)
    }

    private fun startServiceCall(noteId: Long, start: () -> Unit): Boolean = try {
        start()
        true
    } catch (notAllowed: ForegroundServiceStartNotAllowedException) {
        Timber.tag(LOG_TAG).w(notAllowed, "FGS not allowed for note %d", noteId)
        false
    }

    private companion object {
        const val LOG_TAG = "RecordingProcess"
        const val IMPORT_NOTE_ID = 0L
    }
}
