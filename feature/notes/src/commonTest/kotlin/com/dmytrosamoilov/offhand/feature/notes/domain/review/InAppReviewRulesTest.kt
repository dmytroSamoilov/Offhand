package com.dmytrosamoilov.offhand.feature.notes.domain.review

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class InAppReviewRulesTest {

    private val firstAskAt = 1_000.days.inWholeMilliseconds

    @Test
    fun `each platform gets its own spacing and developer builds the fast one`() {
        assertEquals(InAppReviewRules.ANDROID, InAppReviewRules.of(buildInfo(platform = "android")))
        assertEquals(InAppReviewRules.IOS, InAppReviewRules.of(buildInfo(platform = "ios")))
        assertEquals(InAppReviewRules.DEBUG, InAppReviewRules.of(buildInfo(platform = "ios", isDeveloperBuild = true)))
    }

    @Test
    fun `android asks again after 33 days and not before`() {
        assertFalse(isDueAfterFirstAsk(InAppReviewRules.ANDROID, 32.days.inWholeMilliseconds))
        assertTrue(isDueAfterFirstAsk(InAppReviewRules.ANDROID, 33.days.inWholeMilliseconds))
    }

    @Test
    fun `ios asks again after 14 days and not before`() {
        assertFalse(isDueAfterFirstAsk(InAppReviewRules.IOS, 13.days.inWholeMilliseconds))
        assertTrue(isDueAfterFirstAsk(InAppReviewRules.IOS, 14.days.inWholeMilliseconds))
    }

    @Test
    fun `the third ask still fits the burst and the fourth waits for the cooldown`() {
        val policy = InAppReviewPolicy(InAppReviewRules.IOS)
        val secondAskAt = firstAskAt + 14.days.inWholeMilliseconds
        val thirdAskAt = firstAskAt + 28.days.inWholeMilliseconds
        val afterFirst = policy.nextStateAfterAttempt(ReviewPromptState(), firstAskAt)
        val afterSecond = policy.nextStateAfterAttempt(afterFirst, secondAskAt)

        assertTrue(policy.shouldRequestReview(3, afterSecond, thirdAskAt))
        val afterThird = policy.nextStateAfterAttempt(afterSecond, thirdAskAt)
        assertFalse(policy.shouldRequestReview(3, afterThird, thirdAskAt + 44.days.inWholeMilliseconds))
        assertTrue(policy.shouldRequestReview(3, afterThird, thirdAskAt + 45.days.inWholeMilliseconds))
    }

    private fun isDueAfterFirstAsk(rules: InAppReviewRules, elapsedMs: Long): Boolean {
        val policy = InAppReviewPolicy(rules)
        val afterFirst = policy.nextStateAfterAttempt(ReviewPromptState(), firstAskAt)
        return policy.shouldRequestReview(3, afterFirst, firstAskAt + elapsedMs)
    }

    private fun buildInfo(platform: String, isDeveloperBuild: Boolean = false) =
        BuildInfo(isDeveloperBuild = isDeveloperBuild, appVersion = "1", platform = platform)
}
