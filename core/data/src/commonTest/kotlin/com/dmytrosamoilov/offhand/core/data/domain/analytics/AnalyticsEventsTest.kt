package com.dmytrosamoilov.offhand.core.data.domain.analytics

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEventsTest {

    @Test
    fun `custom styles are reported without their name`() {
        assertEquals("custom", NoteStyleRef.Custom(42).analyticsName())
        assertEquals("meeting", NoteStyleRef.BuiltIn(NotePreset.MEETING).analyticsName())
        assertEquals(
            AnalyticsEvent("default_style_changed", mapOf("style" to "custom")),
            AnalyticsEvents.defaultStyleChanged(NoteStyleRef.Custom(42)),
        )
    }

    @Test
    fun `durations go out in whole seconds`() {
        val event = AnalyticsEvents.noteReady(NoteSource.IMPORT, durationMs = 95_400, processingMs = 12_999)

        assertEquals("note_ready", event.name)
        assertEquals(mapOf("source" to "import", "duration_s" to 95L, "processing_s" to 12L), event.params)
        assertEquals(0L, AnalyticsEvents.noteRecorded(null, NoteStyleRef.DEFAULT).params["duration_s"])
    }

    @Test
    fun `parameters use only strings and longs`() {
        val events = listOf(
            AnalyticsEvents.shareCompleted("pdf", savedToDevice = true),
            AnalyticsEvents.noteStyleCreated(sections = 3, described = true),
            AnalyticsEvents.backupCreated(notes = 7, withAudio = false),
            AnalyticsEvents.purchaseCompleted(ProPlan.YEARLY, trial = true),
            AnalyticsEvents.paywallShown(ProFeature.DOCUMENT_EXPORT),
            AnalyticsEvents.noteCopied(NoteSection.TRANSCRIPT),
        )

        events.flatMap { it.params.values }.forEach { value ->
            assertTrue(value is String || value is Long, "unsupported value $value")
        }
        assertEquals("save", events[0].params["action"])
        assertEquals("described", events[1].params["source"])
        assertEquals("false", events[2].params["with_audio"])
        assertEquals("document_export", events[4].params["feature"])
    }
}
