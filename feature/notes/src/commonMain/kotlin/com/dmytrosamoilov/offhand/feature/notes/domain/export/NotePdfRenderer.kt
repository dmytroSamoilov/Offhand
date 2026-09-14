package com.dmytrosamoilov.offhand.feature.notes.domain.export

interface NotePdfRenderer {

    suspend fun render(document: NoteDocument, outputPath: String)
}
