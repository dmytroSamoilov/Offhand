package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleErrors

data class NoteStyleEditorUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val noteKind: String = "",
    val language: NoteStyleLanguage = NoteStyleLanguage.RECORDING,
    val sections: List<SectionDraftUi> = listOf(SectionDraftUi()),
    val errors: NoteStyleErrors = NoteStyleErrors(),
    val preview: StylePreviewUi? = null,
    val describe: DescribeStyleUi? = null,
    val isSaved: Boolean = false,
)

data class DescribeStyleUi(
    val description: String = "",
    val status: DescribeStatusUi = DescribeStatusUi.IDLE,
)

enum class DescribeStatusUi {
    IDLE,
    RUNNING,
    MODEL_UNAVAILABLE,
    FAILED,
}

data class SectionDraftUi(
    val heading: String = "",
    val guidance: String = "",
    val format: SectionFormat = SectionFormat.SENTENCES,
)

sealed interface StylePreviewUi {
    data object Running : StylePreviewUi
    data object ModelUnavailable : StylePreviewUi
    data object Failed : StylePreviewUi
    data class Ready(val title: String, val overview: String) : StylePreviewUi
}
