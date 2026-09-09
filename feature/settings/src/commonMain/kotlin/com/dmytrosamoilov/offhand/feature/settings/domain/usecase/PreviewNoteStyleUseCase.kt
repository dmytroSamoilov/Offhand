package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStylePreview
import com.dmytrosamoilov.offhand.core.data.domain.NoteStylePreviewer

class PreviewNoteStyleUseCase(
    private val previewer: NoteStylePreviewer,
) {
    suspend operator fun invoke(style: CustomNoteStyle, sampleTranscript: String): NoteStylePreview? =
        previewer.preview(style, sampleTranscript)
}
