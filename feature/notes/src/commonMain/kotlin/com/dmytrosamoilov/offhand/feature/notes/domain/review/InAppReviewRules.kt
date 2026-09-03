@file:OptIn(ExperimentalObjCName::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.review

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class InAppReviewRules(
    val minSavedRecordings: Int,
    val minInstallAgeMs: Long,
    val burstWindowMs: Long,
    val attemptGapMs: Long,
    val cooldownMs: Long,
    val maxBurstAttempts: Int = 3,
) {
    companion object {
        @property:ObjCName("productionRules")
        val PRODUCTION = InAppReviewRules(
            minSavedRecordings = 3,
            minInstallAgeMs = 7.days.inWholeMilliseconds,
            burstWindowMs = 5.days.inWholeMilliseconds,
            attemptGapMs = 24.hours.inWholeMilliseconds,
            cooldownMs = 45.days.inWholeMilliseconds,
        )

        @property:ObjCName("debugRules")
        val DEBUG = InAppReviewRules(
            minSavedRecordings = 1,
            minInstallAgeMs = 5.minutes.inWholeMilliseconds,
            burstWindowMs = 5.minutes.inWholeMilliseconds,
            attemptGapMs = 1.minutes.inWholeMilliseconds,
            cooldownMs = 10.minutes.inWholeMilliseconds,
        )
    }
}
