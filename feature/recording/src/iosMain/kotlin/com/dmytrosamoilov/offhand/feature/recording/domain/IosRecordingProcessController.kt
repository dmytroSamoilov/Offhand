package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
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

    override fun restructureNote(noteId: Long, preset: NotePreset): Boolean {
        sessionManager.restructureNote(noteId, preset)
        return true
    }
}
