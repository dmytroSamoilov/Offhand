package com.dmytrosamoilov.offhand.feature.recording.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class RecordingProcessControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecordingProcessController {

    override fun startRecording() {
        RecordingService.start(context)
    }

    override fun retryNote(noteId: Long, audioFileName: String): Boolean = startServiceCall(noteId) {
        RecordingService.retryNote(context, noteId, audioFileName)
    }

    override fun restructureNote(noteId: Long, preset: NotePreset): Boolean = startServiceCall(noteId) {
        RecordingService.restructureNote(context, noteId, preset)
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
    }
}
