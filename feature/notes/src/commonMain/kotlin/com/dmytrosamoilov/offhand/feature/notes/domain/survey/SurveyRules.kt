@file:OptIn(ExperimentalObjCName::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.survey

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlin.time.Duration.Companion.days

data class SurveyRules(
    val minReadyNotes: Int,
    val askAgainAfterMs: Long,
    val maxAsks: Int,
) {
    companion object {
        // Asked of people who really use the app, and once more a month after a
        // "Not now", because whoever is still recording then is a regular.
        @property:ObjCName("standardRules")
        val STANDARD = SurveyRules(minReadyNotes = 3, askAgainAfterMs = 30.days.inWholeMilliseconds, maxAsks = 2)

        @property:ObjCName("neverAskRules")
        val NEVER = SurveyRules(minReadyNotes = Int.MAX_VALUE, askAgainAfterMs = Long.MAX_VALUE, maxAsks = 0)
    }
}
