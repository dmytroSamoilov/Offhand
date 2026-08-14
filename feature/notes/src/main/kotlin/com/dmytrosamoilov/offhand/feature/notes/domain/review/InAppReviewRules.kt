package com.dmytrosamoilov.offhand.feature.notes.domain.review

import java.util.concurrent.TimeUnit

data class InAppReviewRules(
    val minSavedRecordings: Int,
    val minInstallAgeMs: Long,
    val burstWindowMs: Long,
    val attemptGapMs: Long,
    val cooldownMs: Long,
    val maxBurstAttempts: Int = 3,
) {
    companion object {
        val PRODUCTION = InAppReviewRules(
            minSavedRecordings = 3,
            minInstallAgeMs = TimeUnit.DAYS.toMillis(7),
            burstWindowMs = TimeUnit.DAYS.toMillis(5),
            attemptGapMs = TimeUnit.HOURS.toMillis(24),
            cooldownMs = TimeUnit.DAYS.toMillis(45),
        )

        val DEBUG = InAppReviewRules(
            minSavedRecordings = 1,
            minInstallAgeMs = TimeUnit.MINUTES.toMillis(5),
            burstWindowMs = TimeUnit.MINUTES.toMillis(5),
            attemptGapMs = TimeUnit.MINUTES.toMillis(1),
            cooldownMs = TimeUnit.MINUTES.toMillis(10),
        )
    }
}
