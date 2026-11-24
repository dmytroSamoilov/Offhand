@file:OptIn(ExperimentalObjCName::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.review

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

data class InAppReviewRules(
    val minSavedRecordings: Int,
    val burstWindowMs: Long,
    val attemptGapMs: Long,
    val cooldownMs: Long,
    val maxBurstAttempts: Int = 3,
) {
    companion object {
        // Three asks, spaced the way each store shows them: Google Play rarely
        // shows its dialog twice within a month, and Apple allows three prompts
        // a year and suggests a week or two between them.
        @property:ObjCName("androidRules")
        val ANDROID = spaced(attemptGapMs = 33.days.inWholeMilliseconds)

        @property:ObjCName("iosRules")
        val IOS = spaced(attemptGapMs = 14.days.inWholeMilliseconds)

        @property:ObjCName("debugRules")
        val DEBUG = InAppReviewRules(
            minSavedRecordings = 1,
            burstWindowMs = 5.minutes.inWholeMilliseconds,
            attemptGapMs = 1.minutes.inWholeMilliseconds,
            cooldownMs = 10.minutes.inWholeMilliseconds,
        )

        fun of(buildInfo: BuildInfo): InAppReviewRules = when {
            buildInfo.isDeveloperBuild -> DEBUG
            buildInfo.platform == IOS_PLATFORM -> IOS
            else -> ANDROID
        }

        private fun spaced(attemptGapMs: Long) = InAppReviewRules(
            minSavedRecordings = 3,
            burstWindowMs = attemptGapMs * BURST_ATTEMPTS,
            attemptGapMs = attemptGapMs,
            cooldownMs = 45.days.inWholeMilliseconds,
            maxBurstAttempts = BURST_ATTEMPTS,
        )

        private const val BURST_ATTEMPTS = 3
        private const val IOS_PLATFORM = "ios"
    }
}
