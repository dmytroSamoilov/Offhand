package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import com.dmytrosamoilov.offhand.feature.notes.R
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

interface InAppReviewLauncher {

    suspend fun launch(activity: Activity): Boolean
}

// The Play API never reveals whether the dialog was shown — success here only
// means Google accepted the request; callers treat failure as "retry later".
class PlayInAppReviewLauncher : InAppReviewLauncher {

    override suspend fun launch(activity: Activity): Boolean = try {
        val reviewManager = ReviewManagerFactory.create(activity)
        val reviewInfo = reviewManager.requestReview()
        reviewManager.launchReview(activity, reviewInfo)
        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Timber.tag(REVIEW_LOG_TAG).w(failure, "In-app review request failed, retrying on a later note")
        false
    }
}

// Debug stand-in: sideloaded builds never render the real Play dialog, so this
// makes the trigger visible. Dismissing outside counts the attempt, matching
// how the real API reports success without ever showing anything.
class FakeInAppReviewLauncher(
    private val rules: InAppReviewRules,
) : InAppReviewLauncher {

    override suspend fun launch(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            val dialog = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.notes_fake_review_title))
                .setMessage(rulesSummary(activity))
                .setPositiveButton(R.string.notes_fake_review_success) { _, _ ->
                    continuation.resume(true)
                }
                .setNegativeButton(R.string.notes_fake_review_failure) { _, _ ->
                    continuation.resume(false)
                }
                .setOnCancelListener { continuation.resume(true) }
                .show()
            continuation.invokeOnCancellation { dialog.dismiss() }
        }

    private fun rulesSummary(activity: Activity): String = activity.getString(
        R.string.notes_fake_review_body,
        rules.minSavedRecordings,
        TimeUnit.MILLISECONDS.toMinutes(rules.minInstallAgeMs),
        TimeUnit.MILLISECONDS.toMinutes(rules.attemptGapMs),
        TimeUnit.MILLISECONDS.toMinutes(rules.burstWindowMs),
        TimeUnit.MILLISECONDS.toMinutes(rules.cooldownMs),
    )
}

internal fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private const val REVIEW_LOG_TAG = "InAppReview"
