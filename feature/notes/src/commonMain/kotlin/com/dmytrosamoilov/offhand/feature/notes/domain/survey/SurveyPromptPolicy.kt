package com.dmytrosamoilov.offhand.feature.notes.domain.survey

import com.dmytrosamoilov.offhand.core.data.domain.SurveyState

class SurveyPromptPolicy(
    private val rules: SurveyRules,
) {

    // The answer travels as an analytics event, so asking someone who shares
    // nothing would collect an answer that goes nowhere.
    fun shouldAsk(
        state: SurveyState,
        readyNoteTimesMs: List<Long>,
        isSharingUsage: Boolean,
        nowMs: Long,
    ): Boolean = isSharingUsage &&
        !state.isAnswered &&
        readyNoteTimesMs.size >= rules.minReadyNotes &&
        isAskAllowed(state, readyNoteTimesMs, nowMs)

    private fun isAskAllowed(state: SurveyState, readyNoteTimesMs: List<Long>, nowMs: Long): Boolean {
        val skippedAtMs = state.skippedAtMs
        return when {
            state.skipCount >= rules.maxAsks -> false
            state.skipCount == 0 || skippedAtMs == null -> true
            else -> nowMs - skippedAtMs >= rules.askAgainAfterMs && readyNoteTimesMs.any { it > skippedAtMs }
        }
    }
}
