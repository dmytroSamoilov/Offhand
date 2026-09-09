package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController

class IosRecordingProcessController(
    private val sessionManager: RecordingSessionManager,
) : RecordingProcessController {

    override fun startRecording() {
        sessionManager.start()
    }

    override fun retryNote(noteId: Long, audioFileName: String): Boolean {
        sessionManager.retryNote(noteId, audioFileName)
        return true
    }

    override fun restructureNote(noteId: Long, style: NoteStyleRef): Boolean {
        sessionManager.restructureNote(noteId, style)
        return true
    }
}
