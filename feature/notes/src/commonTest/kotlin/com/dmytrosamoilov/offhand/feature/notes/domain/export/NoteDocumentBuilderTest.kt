@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.export

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

class NoteDocumentBuilderTest {

    private val labels = NoteShareLabels(
        title = "Title",
        date = "Date",
        overview = "Overview",
        transcript = "Transcript",
        recorded = "Recorded",
        duration = "Duration",
        createdWith = "Created with Offhand",
        exported = "Exported",
    )

    private val builder = NoteDocumentBuilder(
        labelsProvider = object : NoteShareLabelsProvider {
            override fun labels(): NoteShareLabels = labels
            override fun fallbackTitle(): String = "Recording"
        },
        dateLabelFormatter = object : DateLabelFormatter {
            override fun dateTime(dateTime: LocalDateTime): String = "${dateTime.year}-${dateTime.month.number}"
            override fun day(date: LocalDate): String = date.toString()
            override fun time(dateTime: LocalDateTime): String = "${dateTime.hour}:${dateTime.minute}"
        },
        appIcon = object : AppIconProvider {
            override fun pngBytes(): ByteArray? = byteArrayOf(9)
        },
        clock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(1_800_000_000_000)
        },
    )

    private val note = Note(
        id = 1,
        title = "",
        body = "## Topics\n- Budget",
        transcript = "We approved the budget today.",
        createdAtEpochMs = 1_750_000_000_000,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        durationMs = 65_000,
        style = NoteStyleRef.Custom(7),
    )

    @Test
    fun `document carries fallback title details sections and footer`() = runTest {
        val document = builder.build(note)

        assertEquals("Recording", document.title)
        assertEquals(listOf("Recorded", "Duration"), document.details.map { it.label })
        assertEquals("1 m 05 s", document.details[1].value)
        assertEquals(listOf("Overview", "Transcript"), document.sections.map { it.heading })
        assertEquals(DocumentBlock.Heading("Topics"), document.sections[0].blocks[0])
        assertEquals("Created with Offhand", document.footer.sourceLine)
        assertEquals("Exported: 2027-1", document.footer.exportedLine)
    }

    @Test
    fun `empty parts are skipped`() = runTest {
        val document = builder.build(note.copy(body = "", durationMs = null))

        assertEquals(listOf("Recorded"), document.details.map { it.label })
        assertEquals(listOf("Transcript"), document.sections.map { it.heading })
    }
}
