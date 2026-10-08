package com.dmytrosamoilov.offhand.feature.notes.domain.review

import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState

class InAppReviewPolicy(
    private val rules: InAppReviewRules,
) {

    fun shouldRequestReview(
        savedRecordingsCount: Int,
        state: ReviewPromptState,
        nowMs: Long,
    ): Boolean = hasEnoughRecordings(savedRecordingsCount) && isAttemptDue(state, nowMs)

    fun nextStateAfterAttempt(state: ReviewPromptState, nowMs: Long): ReviewPromptState =
        if (isBurstActive(state, nowMs)) {
            state.copy(attemptCount = state.attemptCount + 1, lastAttemptAtMs = nowMs)
        } else {
            ReviewPromptState(burstStartedAtMs = nowMs, attemptCount = 1, lastAttemptAtMs = nowMs)
        }

    private fun isAttemptDue(state: ReviewPromptState, nowMs: Long): Boolean =
        if (isBurstActive(state, nowMs)) {
            nowMs - state.lastAttemptAtMs >= rules.attemptGapMs
        } else {
            state.lastAttemptAtMs == NEVER || nowMs - state.lastAttemptAtMs >= rules.cooldownMs
        }

    private fun isBurstActive(state: ReviewPromptState, nowMs: Long): Boolean =
        state.burstStartedAtMs != NEVER &&
            nowMs - state.burstStartedAtMs < rules.burstWindowMs &&
            state.attemptCount < rules.maxBurstAttempts

    private fun hasEnoughRecordings(savedRecordingsCount: Int): Boolean =
        savedRecordingsCount >= rules.minSavedRecordings

    private companion object {
        const val NEVER = 0L
    }
}
