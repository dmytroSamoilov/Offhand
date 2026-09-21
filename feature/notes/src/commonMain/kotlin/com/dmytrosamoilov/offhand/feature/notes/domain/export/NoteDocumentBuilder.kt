@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.export

import com.dmytrosamoilov.offhand.core.common.DurationFormatter
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NoteDocumentBuilder(
    private val labelsProvider: NoteShareLabelsProvider,
    private val dateLabelFormatter: DateLabelFormatter,
    private val appIcon: AppIconProvider,
    private val clock: Clock = Clock.System,
) {

    suspend fun build(note: Note): NoteDocument {
        val labels = labelsProvider.labels()
        return NoteDocument(
            title = note.title.ifBlank { labelsProvider.fallbackTitle() },
            details = details(note, labels),
            sections = sections(note, labels),
            footer = DocumentFooter(
                sourceLine = labels.createdWith,
                exportedLine = "${labels.exported}: ${dateLabelFormatter.dateTime(clock.now().local())}",
                iconPng = appIcon.pngBytes(),
            ),
        )
    }

    private fun details(note: Note, labels: NoteShareLabels): List<DocumentDetail> = buildList {
        add(DocumentDetail(labels.recorded, dateLabelFormatter.dateTime(Instant.fromEpochMilliseconds(note.createdAtEpochMs).local())))
        note.durationMs?.let { add(DocumentDetail(labels.duration, DurationFormatter.format(it))) }
    }

    private fun sections(note: Note, labels: NoteShareLabels): List<DocumentSection> = buildList {
        if (note.body.isNotBlank()) add(DocumentSection(labels.overview, MarkdownBlockParser.parse(note.body)))
        if (note.transcript.isNotBlank()) add(DocumentSection(labels.transcript, MarkdownBlockParser.parse(note.transcript)))
    }

    private fun Instant.local(): LocalDateTime = toLocalDateTime(TimeZone.currentSystemDefault())
}
