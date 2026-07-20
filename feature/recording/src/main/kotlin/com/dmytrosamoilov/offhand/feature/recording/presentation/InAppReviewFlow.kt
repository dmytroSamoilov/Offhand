package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlin.coroutines.cancellation.CancellationException
import timber.log.Timber

// The Play API never reveals whether the dialog was shown — success here only
// means Google accepted the request; callers treat failure as "retry later".
internal suspend fun requestInAppReview(activity: Activity): Boolean = try {
    val reviewManager = ReviewManagerFactory.create(activity)
    val reviewInfo = reviewManager.requestReview()
    reviewManager.launchReview(activity, reviewInfo)
    true
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Exception) {
    Timber.tag(REVIEW_LOG_TAG).w(failure, "In-app review request failed, retrying on a later save")
    false
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
