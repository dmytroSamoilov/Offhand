package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialPolicy
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRecord
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialSettlement
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

// Runs once per cold start. An install with no trial on record starts its
// fourteen days now. A record whose start lies further in the future than
// the tolerance means the clock was set back to stretch the trial, and the
// trial is removed for good. The answer is what this launch owes the user:
// the welcome to a trial that just started (only when the trial is what
// makes them Pro, so not for a buyer, the grant or the free override), or
// the message that it ended or was removed.
class SettleProTrialUseCase(
    private val trialRepository: ProTrialRepository,
    private val proStatusRepository: ProStatusRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    suspend operator fun invoke(): ProTrialSettlement {
        val now = nowMs()
        val record = trialRepository.readRecord()
        when {
            record == null -> start(now)
            !record.isRevoked && ProTrialPolicy.isClockSetBack(record, now) -> revoke(record)
        }
        trialRepository.rememberCurrentTime()
        return ProTrialSettlement(
            isTrialOffered = record == null && currentPlan() == ProPlan.TRIAL,
            notice = pendingNotice(),
        )
    }

    private suspend fun currentPlan(): ProPlan = proStatusRepository.observeStatus().first().plan

    private suspend fun start(now: Long) {
        trialRepository.writeRecord(ProTrialRecord(startedAtMs = now))
        analyticsTracker.track(AnalyticsEvents.proTrialStarted())
    }

    private suspend fun revoke(record: ProTrialRecord) {
        trialRepository.writeRecord(record.copy(isRevoked = true))
        analyticsTracker.track(AnalyticsEvents.proTrialRevoked())
    }

    // Someone who bought a plan or holds the grant never hears about the
    // trial, now or after a later lapse.
    private suspend fun pendingNotice(): ProTrialNotice? {
        val notice = when (trialRepository.currentState()) {
            ProTrialState.Ended -> ProTrialNotice.ENDED
            ProTrialState.Revoked -> ProTrialNotice.REMOVED
            ProTrialState.NotStarted, is ProTrialState.Active -> return null
        }
        if (trialRepository.isNoticeSeen(notice)) return null
        if (proStatusRepository.observeStatus().first().isPro) {
            trialRepository.markNoticeSeen(notice)
            return null
        }
        if (notice == ProTrialNotice.ENDED) analyticsTracker.track(AnalyticsEvents.proTrialEnded())
        return notice
    }
}
