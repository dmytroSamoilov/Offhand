package com.dmytrosamoilov.offhand.core.data.domain

interface RecordingProcessController {

    fun startRecording()

    fun retryNote(noteId: Long, audioFileName: String): Boolean

    fun restructureNote(noteId: Long, preset: NotePreset): Boolean
}
