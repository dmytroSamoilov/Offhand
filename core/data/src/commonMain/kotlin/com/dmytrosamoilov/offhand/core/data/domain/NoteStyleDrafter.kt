package com.dmytrosamoilov.offhand.core.data.domain

data class NoteStyleDraft(
    val name: String,
    val noteKind: String,
    val sections: List<NoteStyleSection>,
)

interface NoteStyleDrafter {

    suspend fun draft(description: String): NoteStyleDraft?
}

class NoteStyleDraftException : Exception("The model did not return a usable note style")
