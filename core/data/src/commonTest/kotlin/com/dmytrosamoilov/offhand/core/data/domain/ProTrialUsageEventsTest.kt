package com.dmytrosamoilov.offhand.core.data.domain

import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.proTrialUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProTrialUsageEventsTest {

    @Test
    fun `a note in a Pro style counts under that style and Summary does not`() {
        val meeting = NoteStyleRef.BuiltIn(NotePreset.MEETING)

        assertEquals(once(ProTrialUsage.MEETING_NOTES), AnalyticsEvents.noteRecorded(1_000, meeting).proTrialUsage())
        assertEquals(once(ProTrialUsage.LEGAL_NOTES), AnalyticsEvents.noteStyleChanged(NoteStyleRef.BuiltIn(NotePreset.LEGAL)).proTrialUsage())
        assertEquals(once(ProTrialUsage.VISIT_NOTES), AnalyticsEvents.noteRecorded(1_000, NoteStyleRef.BuiltIn(NotePreset.VISIT)).proTrialUsage())
        assertEquals(once(ProTrialUsage.CUSTOM_STYLE_NOTES), AnalyticsEvents.noteRecorded(1_000, NoteStyleRef.Custom(4)).proTrialUsage())
        assertNull(AnalyticsEvents.noteRecorded(1_000, NoteStyleRef.DEFAULT).proTrialUsage())
    }

    @Test
    fun `document exports count and plain text or audio does not`() {
        assertEquals(once(ProTrialUsage.PDF_EXPORTS), AnalyticsEvents.shareCompleted("pdf", false).proTrialUsage())
        assertEquals(once(ProTrialUsage.WORD_EXPORTS), AnalyticsEvents.shareCompleted("docx", true).proTrialUsage())
        assertNull(AnalyticsEvents.shareCompleted("text", false).proTrialUsage())
        assertNull(AnalyticsEvents.shareCompleted("audio", false).proTrialUsage())
    }

    @Test
    fun `an import counts every file`() {
        assertEquals(ProTrialUsageCount(ProTrialUsage.IMPORTS, 3), AnalyticsEvents.audioImportStarted(files = 3, videos = 1).proTrialUsage())
    }

    @Test
    fun `calendar events and folder styles and audio backups count`() {
        assertEquals(once(ProTrialUsage.CALENDAR_EVENTS), AnalyticsEvents.suggestionAdded().proTrialUsage())
        assertEquals(once(ProTrialUsage.FOLDER_STYLES), AnalyticsEvents.folderStyleChanged(NoteStyleRef.Custom(1)).proTrialUsage())
        assertNull(AnalyticsEvents.folderStyleChanged(null).proTrialUsage())
        assertEquals(once(ProTrialUsage.AUDIO_BACKUPS), AnalyticsEvents.backupCreated(notes = 4, withAudio = true).proTrialUsage())
        assertNull(AnalyticsEvents.backupCreated(notes = 4, withAudio = false).proTrialUsage())
    }

    @Test
    fun `everything else is not usage`() {
        assertNull(AnalyticsEvents.noteDeleted().proTrialUsage())
    }

    private fun once(usage: ProTrialUsage) = ProTrialUsageCount(usage, 1)
}
