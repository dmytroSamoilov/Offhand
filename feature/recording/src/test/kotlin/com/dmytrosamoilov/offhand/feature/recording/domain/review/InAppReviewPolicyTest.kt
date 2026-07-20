package com.dmytrosamoilov.offhand.feature.recording.domain.review

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewPolicyTest {

    private val policy = InAppReviewPolicy()

    private val now = TimeUnit.DAYS.toMillis(365)
    private val matureInstall = now - TimeUnit.DAYS.toMillis(8)

    private fun shouldRequest(
        savedRecordingsCount: Int = 3,
        installedAtMs: Long = matureInstall,
        lastReviewRequestAtMs: Long = InAppReviewPolicy.NEVER_REQUESTED,
    ): Boolean = policy.shouldRequestReview(
        savedRecordingsCount = savedRecordingsCount,
        installedAtMs = installedAtMs,
        lastReviewRequestAtMs = lastReviewRequestAtMs,
        nowMs = now,
    )

    @Test
    fun `first request fires with three recordings, mature install and no prior request`() {
        assertTrue(shouldRequest())
    }

    @Test
    fun `fewer than three saved recordings never requests`() {
        assertFalse(shouldRequest(savedRecordingsCount = 2))
        assertFalse(shouldRequest(savedRecordingsCount = 0))
    }

    @Test
    fun `more than three saved recordings still requests`() {
        assertTrue(shouldRequest(savedRecordingsCount = 47))
    }

    @Test
    fun `install younger than seven days never requests`() {
        val youngInstall = now - TimeUnit.DAYS.toMillis(6)

        assertFalse(shouldRequest(installedAtMs = youngInstall))
    }

    @Test
    fun `install of exactly seven days requests`() {
        val boundaryInstall = now - TimeUnit.DAYS.toMillis(7)

        assertTrue(shouldRequest(installedAtMs = boundaryInstall))
    }

    @Test
    fun `successful request less than 45 days ago blocks the next one`() {
        val recentRequest = now - TimeUnit.DAYS.toMillis(44)

        assertFalse(shouldRequest(lastReviewRequestAtMs = recentRequest))
    }

    @Test
    fun `successful request 45 or more days ago allows the next one`() {
        val cooldownPassed = now - TimeUnit.DAYS.toMillis(45)

        assertTrue(shouldRequest(lastReviewRequestAtMs = cooldownPassed))
    }

    @Test
    fun `failed request leaves state untouched so the next save retries`() {
        assertTrue(shouldRequest())
        assertTrue(shouldRequest())
    }
}
