package com.dmytrosamoilov.offhand.feature.notes.domain.export

data class NoteDocument(
    val title: String,
    val details: List<DocumentDetail>,
    val sections: List<DocumentSection>,
    val footer: DocumentFooter,
)

data class DocumentDetail(
    val label: String,
    val value: String,
)

data class DocumentSection(
    val heading: String,
    val blocks: List<DocumentBlock>,
)

sealed interface DocumentBlock {
    val text: String

    data class Heading(override val text: String) : DocumentBlock
    data class Paragraph(override val text: String) : DocumentBlock
    data class Bullet(override val text: String) : DocumentBlock
    data class Numbered(val number: Int, override val text: String) : DocumentBlock
}

data class DocumentFooter(
    val sourceLine: String,
    val exportedLine: String,
    val iconPng: ByteArray?,
)
