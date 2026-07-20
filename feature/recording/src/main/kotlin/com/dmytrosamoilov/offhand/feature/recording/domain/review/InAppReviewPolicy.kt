package com.dmytrosamoilov.offhand.feature.recording.domain.review

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppReviewPolicy @Inject constructor() {

    fun shouldRequestReview(
        savedRecordingsCount: Int,
        installedAtMs: Long,
        lastReviewRequestAtMs: Long,
        nowMs: Long,
    ): Boolean = hasEnoughRecordings(savedRecordingsCount) &&
        isInstallMatureEnough(installedAtMs, nowMs) &&
        isOutsideCooldown(lastReviewRequestAtMs, nowMs)

    private fun hasEnoughRecordings(savedRecordingsCount: Int): Boolean =
        savedRecordingsCount >= MIN_SAVED_RECORDINGS

    private fun isInstallMatureEnough(installedAtMs: Long, nowMs: Long): Boolean =
        nowMs - installedAtMs >= MIN_INSTALL_AGE_MS

    private fun isOutsideCooldown(lastReviewRequestAtMs: Long, nowMs: Long): Boolean =
        lastReviewRequestAtMs == NEVER_REQUESTED ||
            nowMs - lastReviewRequestAtMs >= REQUEST_COOLDOWN_MS

    companion object {
        const val NEVER_REQUESTED = 0L
        const val MIN_SAVED_RECORDINGS = 3
        val MIN_INSTALL_AGE_MS = TimeUnit.DAYS.toMillis(7)
        val REQUEST_COOLDOWN_MS = TimeUnit.DAYS.toMillis(45)
    }
}
