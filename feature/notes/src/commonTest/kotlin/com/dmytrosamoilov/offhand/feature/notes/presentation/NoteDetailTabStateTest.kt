package com.dmytrosamoilov.offhand.feature.notes.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteDetailTabStateTest {

    @Test
    fun `a note that is still processing opens on the transcript`() {
        val tabs = NoteDetailTabState.openedFor(NoteStatusUi.PROCESSING)

        assertEquals(NoteDetailTabUi.TRANSCRIPT, tabs.tab)
        assertFalse(tabs.isSummaryReadyMarked)
    }

    @Test
    fun `a ready note opens on the summary`() {
        val tabs = NoteDetailTabState.openedFor(NoteStatusUi.READY)

        assertEquals(NoteDetailTabUi.SUMMARY, tabs.tab)
        assertFalse(tabs.isSummaryReadyMarked)
    }

    @Test
    fun `a failed or paused note opens on the summary where its message is`() {
        assertEquals(NoteDetailTabUi.SUMMARY, NoteDetailTabState.openedFor(NoteStatusUi.FAILED).tab)
        assertEquals(NoteDetailTabUi.SUMMARY, NoteDetailTabState.openedFor(NoteStatusUi.INTERRUPTED).tab)
    }

    @Test
    fun `a summary that arrives while the transcript is open marks its tab and keeps the transcript`() {
        val opened = NoteDetailTabState.openedFor(NoteStatusUi.PROCESSING)

        val tabs = opened.onStatusChanged(NoteStatusUi.PROCESSING, NoteStatusUi.READY)

        assertEquals(NoteDetailTabUi.TRANSCRIPT, tabs.tab)
        assertTrue(tabs.isSummaryReadyMarked)
    }

    @Test
    fun `a summary that arrives while its own tab is open needs no mark`() {
        val onSummary = NoteDetailTabState(tab = NoteDetailTabUi.SUMMARY)

        val tabs = onSummary.onStatusChanged(NoteStatusUi.PROCESSING, NoteStatusUi.READY)

        assertEquals(NoteDetailTabState(tab = NoteDetailTabUi.SUMMARY), tabs)
    }

    @Test
    fun `opening the summary clears its mark`() {
        val marked = NoteDetailTabState(tab = NoteDetailTabUi.TRANSCRIPT, isSummaryReadyMarked = true)

        val tabs = marked.onTabSelected(NoteDetailTabUi.SUMMARY)

        assertEquals(NoteDetailTabState(tab = NoteDetailTabUi.SUMMARY), tabs)
    }

    @Test
    fun `tapping the transcript again keeps the mark`() {
        val marked = NoteDetailTabState(tab = NoteDetailTabUi.TRANSCRIPT, isSummaryReadyMarked = true)

        assertEquals(marked, marked.onTabSelected(NoteDetailTabUi.TRANSCRIPT))
    }

    @Test
    fun `a note that is written again loses the mark and keeps its tab`() {
        val marked = NoteDetailTabState(tab = NoteDetailTabUi.TRANSCRIPT, isSummaryReadyMarked = true)

        val tabs = marked.onStatusChanged(NoteStatusUi.READY, NoteStatusUi.PROCESSING)

        assertEquals(NoteDetailTabState(tab = NoteDetailTabUi.TRANSCRIPT), tabs)
    }

    @Test
    fun `a ready note that only changes its text is left alone`() {
        val onTranscript = NoteDetailTabState(tab = NoteDetailTabUi.TRANSCRIPT)

        assertEquals(onTranscript, onTranscript.onStatusChanged(NoteStatusUi.READY, NoteStatusUi.READY))
    }
}
