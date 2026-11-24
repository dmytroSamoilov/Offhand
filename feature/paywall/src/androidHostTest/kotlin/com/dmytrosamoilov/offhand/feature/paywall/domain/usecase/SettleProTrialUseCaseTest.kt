package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialPolicy
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecord
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialSettlement
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettleProTrialUseCaseTest {

    private var now = 1_000_000_000_000L
    private val trial = MemoryProTrialRepository { now }
    private val proStatus: ProStatusRepository = mockk {
        every { observeStatus() } returns flowOf(ProStatus.FREE)
    }
    private val tracked = mutableListOf<String>()
    private val tracker = object : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) {
            tracked += event.name
        }
    }
    private val useCase = SettleProTrialUseCase(trial, proStatus, tracker) { now }

    @Test
    fun `an install with no record starts its trial now`() = runTest {
        every { proStatus.observeStatus() } returns flowOf(ProStatus.trial(endsAtMs = now))

        assertEquals(ProTrialSettlement(), useCase())

        assertEquals(ProTrialRecord(startedAtMs = now), trial.record)
        assertEquals(listOf("pro_trial_started"), tracked)
    }

    @Test
    fun `a running trial is left alone on later launches`() = runTest {
        trial.record = ProTrialRecord(startedAtMs = now)
        now += 5.days.inWholeMilliseconds

        assertEquals(ProTrialSettlement(), useCase())

        assertEquals(ProTrialRecord(startedAtMs = 1_000_000_000_000L), trial.record)
        assertEquals(emptyList<String>(), tracked)
    }

    @Test
    fun `the end is announced once`() = runTest {
        trial.record = ProTrialRecord(startedAtMs = now)
        now += 14.days.inWholeMilliseconds

        assertEquals(ProTrialNotice.ENDED, useCase().notice)
        trial.markNoticeSeen(ProTrialNotice.ENDED)

        assertEquals(ProTrialSettlement(), useCase())
        assertEquals(listOf("pro_trial_ended"), tracked)
    }

    @Test
    fun `a Pro user never hears that the trial ended`() = runTest {
        every { proStatus.observeStatus() } returns flowOf(ProStatus.LIFETIME)
        trial.record = ProTrialRecord(startedAtMs = now)
        now += 20.days.inWholeMilliseconds

        assertEquals(ProTrialSettlement(), useCase())

        every { proStatus.observeStatus() } returns flowOf(ProStatus.FREE)
        assertEquals(ProTrialSettlement(), useCase())
    }

    @Test
    fun `a clock set back past the tolerance removes the trial for good`() = runTest {
        trial.record = ProTrialRecord(startedAtMs = now)
        now -= 72.hours.inWholeMilliseconds + 1

        assertEquals(ProTrialNotice.REMOVED, useCase().notice)
        assertEquals(true, trial.record?.isRevoked)
        assertEquals(listOf("pro_trial_revoked"), tracked)

        now += 10.days.inWholeMilliseconds
        assertEquals(ProTrialNotice.REMOVED, useCase().notice)
        assertEquals(listOf("pro_trial_revoked"), tracked)
    }

    @Test
    fun `a clock within the tolerance keeps the trial`() = runTest {
        trial.record = ProTrialRecord(startedAtMs = now)
        now -= 72.hours.inWholeMilliseconds

        assertEquals(ProTrialSettlement(), useCase())
        assertEquals(false, trial.record?.isRevoked)
    }
}

private class MemoryProTrialRepository(private val nowMs: () -> Long) : ProTrialRepository {
    var record: ProTrialRecord? = null
    private val seen = mutableSetOf<ProTrialNotice>()

    override fun observeState(): Flow<ProTrialState> = flowOf(ProTrialPolicy.stateOf(record, nowMs()))
    override suspend fun currentState(): ProTrialState = ProTrialPolicy.stateOf(record, nowMs())
    override suspend fun readRecord(): ProTrialRecord? = record
    override suspend fun writeRecord(record: ProTrialRecord) {
        this.record = record
    }
    override suspend fun rememberCurrentTime() = Unit
    override suspend fun isNoticeSeen(notice: ProTrialNotice): Boolean = notice in seen
    override suspend fun markNoticeSeen(notice: ProTrialNotice) {
        seen += notice
    }
    override suspend fun keepInSync() = Unit
}
