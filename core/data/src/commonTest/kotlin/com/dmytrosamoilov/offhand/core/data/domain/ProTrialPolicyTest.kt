package com.dmytrosamoilov.offhand.core.data.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ProTrialPolicyTest {

    private val start = 1_000_000_000_000L
    private val record = ProTrialRecord(startedAtMs = start)

    @Test
    fun `the trial runs for fourteen days from its start`() {
        val end = start + 14.days.inWholeMilliseconds

        assertEquals(ProTrialState.Active(endsAtMs = end), ProTrialPolicy.stateOf(record, start))
        assertEquals(ProTrialState.Active(endsAtMs = end), ProTrialPolicy.stateOf(record, end - 1))
        assertEquals(ProTrialState.Ended, ProTrialPolicy.stateOf(record, end))
    }

    @Test
    fun `no record means not started and a removed trial stays removed`() {
        assertEquals(ProTrialState.NotStarted, ProTrialPolicy.stateOf(null, start))
        assertEquals(ProTrialState.Revoked, ProTrialPolicy.stateOf(record.copy(isRevoked = true), start))
    }

    @Test
    fun `a clock up to three days before the start is tolerated`() {
        assertFalse(ProTrialPolicy.isClockSetBack(record, start - 72.hours.inWholeMilliseconds))
        assertTrue(ProTrialPolicy.isClockSetBack(record, start - 72.hours.inWholeMilliseconds - 1))
        assertFalse(ProTrialPolicy.isClockSetBack(record, start + 30.days.inWholeMilliseconds))
    }

    @Test
    fun `merging keeps the earlier start and any removal`() {
        val later = ProTrialRecord(startedAtMs = start + 5, isRevoked = true)

        assertEquals(ProTrialRecord(startedAtMs = start, isRevoked = true), ProTrialPolicy.merge(record, later))
        assertEquals(record, ProTrialPolicy.merge(record, null))
        assertEquals(later, ProTrialPolicy.merge(null, later))
        assertNull(ProTrialPolicy.merge(null, null))
    }

    @Test
    fun `days left round up and never exceed the trial`() {
        val end = start + 14.days.inWholeMilliseconds

        assertEquals(14, ProTrialPolicy.daysLeft(end, start))
        assertEquals(14, ProTrialPolicy.daysLeft(end, start - 2.days.inWholeMilliseconds))
        assertEquals(1, ProTrialPolicy.daysLeft(end, end - 1.hours.inWholeMilliseconds))
        assertEquals(2, ProTrialPolicy.daysLeft(end, end - 25.hours.inWholeMilliseconds))
    }

    @Test
    fun `the elapsed share grows from nothing to all of it`() {
        val end = start + 14.days.inWholeMilliseconds

        assertEquals(0f, ProTrialPolicy.elapsedFraction(end, start))
        assertEquals(0.5f, ProTrialPolicy.elapsedFraction(end, start + 7.days.inWholeMilliseconds))
        assertEquals(1f, ProTrialPolicy.elapsedFraction(end, end + 1))
    }
}
