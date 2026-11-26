package com.dmytrosamoilov.offhand.feature.notes.domain.survey

import com.dmytrosamoilov.offhand.core.data.domain.SurveyState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class SurveyPromptPolicyTest {

    private val policy = SurveyPromptPolicy(SurveyRules.STANDARD)
    private val now = 1_000.days.inWholeMilliseconds
    private val threeNotes = listOf(now - 3.days.inWholeMilliseconds, now - 2.days.inWholeMilliseconds, now - 1.days.inWholeMilliseconds)

    @Test
    fun `asks from the third finished note and not before`() {
        assertFalse(shouldAsk(readyNoteTimesMs = threeNotes.take(2)))
        assertTrue(shouldAsk(readyNoteTimesMs = threeNotes))
    }

    @Test
    fun `never asks someone who shares no usage`() {
        assertFalse(shouldAsk(isSharingUsage = false))
    }

    @Test
    fun `an answered question is never asked again`() {
        assertFalse(shouldAsk(state = SurveyState(answer = "meetings")))
    }

    @Test
    fun `after the first not now it waits`() {
        val skippedAt = now - 29.days.inWholeMilliseconds
        val state = SurveyState(skippedAtMs = skippedAt, skipCount = 1)

        assertFalse(shouldAsk(state = state, readyNoteTimesMs = threeNotes + (now - 1)))
    }

    @Test
    fun `it comes back 30 days later when a note was finished since`() {
        val skippedAt = now - 30.days.inWholeMilliseconds
        val state = SurveyState(skippedAtMs = skippedAt, skipCount = 1)
        val olderNotes = List(3) { skippedAt - 1.days.inWholeMilliseconds * (it + 1) }

        assertTrue(shouldAsk(state = state, readyNoteTimesMs = olderNotes + (skippedAt + 1)))
    }

    @Test
    fun `it stays away after 30 days when nothing was recorded since`() {
        val skippedAt = now - 45.days.inWholeMilliseconds
        val state = SurveyState(skippedAtMs = skippedAt, skipCount = 1)
        val olderNotes = List(3) { skippedAt - 1.days.inWholeMilliseconds * (it + 1) }

        assertFalse(shouldAsk(state = state, readyNoteTimesMs = olderNotes))
    }

    @Test
    fun `a second not now ends it for good`() {
        val skippedAt = now - 400.days.inWholeMilliseconds
        val state = SurveyState(skippedAtMs = skippedAt, skipCount = 2)

        assertFalse(shouldAsk(state = state, readyNoteTimesMs = threeNotes))
    }

    @Test
    fun `the never rules ask nobody`() {
        val never = SurveyPromptPolicy(SurveyRules.NEVER)

        assertFalse(never.shouldAsk(SurveyState(), List(50) { now - it }, isSharingUsage = true, nowMs = now))
    }

    private fun shouldAsk(
        state: SurveyState = SurveyState(),
        readyNoteTimesMs: List<Long> = threeNotes,
        isSharingUsage: Boolean = true,
    ): Boolean = policy.shouldAsk(state, readyNoteTimesMs, isSharingUsage, now)
}
