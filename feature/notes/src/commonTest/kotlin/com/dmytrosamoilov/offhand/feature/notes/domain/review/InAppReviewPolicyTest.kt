package com.dmytrosamoilov.offhand.feature.notes.domain.review

import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class InAppReviewPolicyTest {

    private val policy = InAppReviewPolicy(InAppReviewRules.PRODUCTION)
    private val now = 1_000.days.inWholeMilliseconds
    private val matureInstall = now - 30.days.inWholeMilliseconds

    private fun shouldRequest(
        savedRecordingsCount: Int = 5,
        installedAtMs: Long = matureInstall,
        state: ReviewPromptState = ReviewPromptState(),
        nowMs: Long = now,
    ): Boolean = policy.shouldRequestReview(
        savedRecordingsCount = savedRecordingsCount,
        installedAtMs = installedAtMs,
        state = state,
        nowMs = nowMs,
    )

    @Test
    fun `too few recordings blocks the request`() {
        assertFalse(shouldRequest(savedRecordingsCount = 2))
    }

    @Test
    fun `young install blocks the request`() {
        assertFalse(shouldRequest(installedAtMs = now - 6.days.inWholeMilliseconds))
    }

    @Test
    fun `all conditions met with no history starts the first burst`() {
        assertTrue(shouldRequest())
    }

    @Test
    fun `second attempt in a burst waits for the attempt gap`() {
        val state = policy.nextStateAfterAttempt(ReviewPromptState(), now)

        assertFalse(shouldRequest(state = state, nowMs = now + 23.hours.inWholeMilliseconds))
        assertTrue(shouldRequest(state = state, nowMs = now + 24.hours.inWholeMilliseconds))
    }

    @Test
    fun `a burst allows at most three attempts`() {
        var state = ReviewPromptState()
        var clock = now
        repeat(3) {
            assertTrue(shouldRequest(state = state, nowMs = clock))
            state = policy.nextStateAfterAttempt(state, clock)
            clock += 1.days.inWholeMilliseconds
        }

        assertFalse(shouldRequest(state = state, nowMs = clock))
    }

    @Test
    fun `a lapsed burst window blocks further attempts until the cooldown passes`() {
        val state = policy.nextStateAfterAttempt(ReviewPromptState(), now)
        val afterWindow = now + 6.days.inWholeMilliseconds

        assertFalse(shouldRequest(state = state, nowMs = afterWindow))
    }

    @Test
    fun `cooldown counts from the last attempt`() {
        var state = ReviewPromptState()
        var clock = now
        repeat(3) {
            state = policy.nextStateAfterAttempt(state, clock)
            clock += 1.days.inWholeMilliseconds
        }
        val lastAttemptAt = state.lastAttemptAtMs

        assertFalse(
            shouldRequest(state = state, nowMs = lastAttemptAt + 44.days.inWholeMilliseconds),
        )
        assertTrue(
            shouldRequest(state = state, nowMs = lastAttemptAt + 45.days.inWholeMilliseconds),
        )
    }

    @Test
    fun `attempt after the cooldown starts a fresh burst`() {
        val exhausted = ReviewPromptState(
            burstStartedAtMs = now - 50.days.inWholeMilliseconds,
            attemptCount = 3,
            lastAttemptAtMs = now - 46.days.inWholeMilliseconds,
        )

        val next = policy.nextStateAfterAttempt(exhausted, now)

        assertEquals(ReviewPromptState(burstStartedAtMs = now, attemptCount = 1, lastAttemptAtMs = now), next)
    }

    @Test
    fun `attempt inside an active burst increments the count`() {
        val started = policy.nextStateAfterAttempt(ReviewPromptState(), now)
        val later = now + 1.days.inWholeMilliseconds

        val next = policy.nextStateAfterAttempt(started, later)

        assertEquals(started.burstStartedAtMs, next.burstStartedAtMs)
        assertEquals(2, next.attemptCount)
        assertEquals(later, next.lastAttemptAtMs)
    }

    @Test
    fun `debug rules collapse the timeline to minutes`() {
        val debugPolicy = InAppReviewPolicy(InAppReviewRules.DEBUG)
        val installedAt = now - 5.minutes.inWholeMilliseconds

        assertTrue(
            debugPolicy.shouldRequestReview(
                savedRecordingsCount = 1,
                installedAtMs = installedAt,
                state = ReviewPromptState(),
                nowMs = now,
            ),
        )
        val afterFirst = debugPolicy.nextStateAfterAttempt(ReviewPromptState(), now)
        assertFalse(
            debugPolicy.shouldRequestReview(
                savedRecordingsCount = 1,
                installedAtMs = installedAt,
                state = afterFirst,
                nowMs = now + 59.seconds.inWholeMilliseconds,
            ),
        )
        assertTrue(
            debugPolicy.shouldRequestReview(
                savedRecordingsCount = 1,
                installedAtMs = installedAt,
                state = afterFirst,
                nowMs = now + 1.minutes.inWholeMilliseconds,
            ),
        )
    }

    @Test
    fun `legacy single-attempt state is honored as a cooldown anchor`() {
        val migrated = ReviewPromptState(lastAttemptAtMs = now - 10.days.inWholeMilliseconds)

        assertFalse(shouldRequest(state = migrated))
        assertTrue(shouldRequest(state = migrated, nowMs = now + 36.days.inWholeMilliseconds))
    }
}
