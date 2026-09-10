package com.dmytrosamoilov.offhand.feature.notes.domain.export

enum class NoteExportFormat(val fileExtension: String, val mimeType: String) {
    TEXT("txt", "text/plain"),
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
}
