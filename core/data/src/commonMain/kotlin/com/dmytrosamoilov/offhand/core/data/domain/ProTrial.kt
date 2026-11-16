package com.dmytrosamoilov.offhand.core.data.domain

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

// The one free trial an install gets: Pro for fourteen days from the first
// launch, with no purchase. Times are epoch milliseconds, so a time-zone
// change never moves the end.
data class ProTrialRecord(
    val startedAtMs: Long,
    val isRevoked: Boolean = false,
)

sealed interface ProTrialState {
    data object NotStarted : ProTrialState
    data class Active(val endsAtMs: Long) : ProTrialState
    data object Ended : ProTrialState
    data object Revoked : ProTrialState
}

enum class ProTrialNotice {
    ENDED,
    REMOVED,
}

object ProTrialPolicy {

    val DURATION_MS = 14.days.inWholeMilliseconds

    // A clock that reads earlier than the trial's start by more than this was
    // set back, which would stretch the trial past its fourteen days.
    val CLOCK_TOLERANCE_MS = 72.hours.inWholeMilliseconds

    fun stateOf(record: ProTrialRecord?, nowMs: Long): ProTrialState = when {
        record == null -> ProTrialState.NotStarted
        record.isRevoked -> ProTrialState.Revoked
        nowMs >= record.startedAtMs + DURATION_MS -> ProTrialState.Ended
        else -> ProTrialState.Active(endsAtMs = record.startedAtMs + DURATION_MS)
    }

    fun isClockSetBack(record: ProTrialRecord, nowMs: Long): Boolean =
        nowMs < record.startedAtMs - CLOCK_TOLERANCE_MS

    // Two copies of the record can disagree after a reinstall: the earlier
    // start is the real one, and a removal is never undone.
    fun merge(first: ProTrialRecord?, second: ProTrialRecord?): ProTrialRecord? {
        if (first == null || second == null) return first ?: second
        return ProTrialRecord(
            startedAtMs = minOf(first.startedAtMs, second.startedAtMs),
            isRevoked = first.isRevoked || second.isRevoked,
        )
    }

    fun daysLeft(endsAtMs: Long, nowMs: Long): Int {
        val remainingMs = (endsAtMs - nowMs).coerceIn(0L, DURATION_MS)
        return ((remainingMs + DAY_MS - 1) / DAY_MS).toInt().coerceAtLeast(1)
    }

    fun elapsedFraction(endsAtMs: Long, nowMs: Long): Float {
        val remainingMs = (endsAtMs - nowMs).coerceIn(0L, DURATION_MS)
        return 1f - remainingMs.toFloat() / DURATION_MS
    }

    private val DAY_MS = 1.days.inWholeMilliseconds
}
