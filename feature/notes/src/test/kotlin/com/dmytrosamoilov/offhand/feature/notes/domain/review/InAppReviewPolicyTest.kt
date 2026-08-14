package com.dmytrosamoilov.offhand.feature.notes.domain.review

import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewPolicyTest {

    private val policy = InAppReviewPolicy(InAppReviewRules.PRODUCTION)
    private val now = TimeUnit.DAYS.toMillis(1_000)
    private val matureInstall = now - TimeUnit.DAYS.toMillis(30)

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
        assertFalse(shouldRequest(installedAtMs = now - TimeUnit.DAYS.toMillis(6)))
    }

    @Test
    fun `all conditions met with no history starts the first burst`() {
        assertTrue(shouldRequest())
    }

    @Test
    fun `second attempt in a burst waits for the attempt gap`() {
        val state = policy.nextStateAfterAttempt(ReviewPromptState(), now)

        assertFalse(shouldRequest(state = state, nowMs = now + TimeUnit.HOURS.toMillis(23)))
        assertTrue(shouldRequest(state = state, nowMs = now + TimeUnit.HOURS.toMillis(24)))
    }

    @Test
    fun `a burst allows at most three attempts`() {
        var state = ReviewPromptState()
        var clock = now
        repeat(3) {
            assertTrue(shouldRequest(state = state, nowMs = clock))
            state = policy.nextStateAfterAttempt(state, clock)
            clock += TimeUnit.DAYS.toMillis(1)
        }

        assertFalse(shouldRequest(state = state, nowMs = clock))
    }

    @Test
    fun `a lapsed burst window blocks further attempts until the cooldown passes`() {
        val state = policy.nextStateAfterAttempt(ReviewPromptState(), now)
        val afterWindow = now + TimeUnit.DAYS.toMillis(6)

        assertFalse(shouldRequest(state = state, nowMs = afterWindow))
    }

    @Test
    fun `cooldown counts from the last attempt`() {
        var state = ReviewPromptState()
        var clock = now
        repeat(3) {
            state = policy.nextStateAfterAttempt(state, clock)
            clock += TimeUnit.DAYS.toMillis(1)
        }
        val lastAttemptAt = state.lastAttemptAtMs

        assertFalse(
            shouldRequest(state = state, nowMs = lastAttemptAt + TimeUnit.DAYS.toMillis(44)),
        )
        assertTrue(
            shouldRequest(state = state, nowMs = lastAttemptAt + TimeUnit.DAYS.toMillis(45)),
        )
    }

    @Test
    fun `attempt after the cooldown starts a fresh burst`() {
        val exhausted = ReviewPromptState(
            burstStartedAtMs = now - TimeUnit.DAYS.toMillis(50),
            attemptCount = 3,
            lastAttemptAtMs = now - TimeUnit.DAYS.toMillis(46),
        )

        val next = policy.nextStateAfterAttempt(exhausted, now)

        assertEquals(ReviewPromptState(burstStartedAtMs = now, attemptCount = 1, lastAttemptAtMs = now), next)
    }

    @Test
    fun `attempt inside an active burst increments the count`() {
        val started = policy.nextStateAfterAttempt(ReviewPromptState(), now)
        val later = now + TimeUnit.DAYS.toMillis(1)

        val next = policy.nextStateAfterAttempt(started, later)

        assertEquals(started.burstStartedAtMs, next.burstStartedAtMs)
        assertEquals(2, next.attemptCount)
        assertEquals(later, next.lastAttemptAtMs)
    }

    @Test
    fun `debug rules collapse the timeline to minutes`() {
        val debugPolicy = InAppReviewPolicy(InAppReviewRules.DEBUG)
        val installedAt = now - TimeUnit.MINUTES.toMillis(5)

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
                nowMs = now + TimeUnit.SECONDS.toMillis(59),
            ),
        )
        assertTrue(
            debugPolicy.shouldRequestReview(
                savedRecordingsCount = 1,
                installedAtMs = installedAt,
                state = afterFirst,
                nowMs = now + TimeUnit.MINUTES.toMillis(1),
            ),
        )
    }

    @Test
    fun `legacy single-attempt state is honored as a cooldown anchor`() {
        val migrated = ReviewPromptState(lastAttemptAtMs = now - TimeUnit.DAYS.toMillis(10))

        assertFalse(shouldRequest(state = migrated))
        assertTrue(shouldRequest(state = migrated, nowMs = now + TimeUnit.DAYS.toMillis(36)))
    }
}
