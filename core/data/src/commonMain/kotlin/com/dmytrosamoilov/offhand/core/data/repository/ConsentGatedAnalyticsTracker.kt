package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.TelemetryConsent
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsSink
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Events exist only for users who agreed to telemetry. An event tracked while
// the answer is not known yet (the stored preferences are still loading, or
// the onboarding question has not been answered) waits on the phone and goes
// out only once the answer is "share"; a "don't share" drops it, and so does
// anything tracked after consent is withdrawn. An event tracked in the moment
// between the answer and the platform switching its reporting on waits the
// same way, so the first events after a "share" are not lost. Developer builds send nothing
// at all, so the owner's own testing never lands in the numbers. Every event
// carries the platform, so exports that merge both streams still tell Android
// and iOS behaviour apart.
class ConsentGatedAnalyticsTracker(
    private val sink: AnalyticsSink,
    userPreferences: UserPreferencesRepository,
    private val buildInfo: BuildInfo,
    scope: CoroutineScope,
) : AnalyticsTracker {

    private val consent = userPreferences.preferences
        .map { it.telemetryConsent }
        .stateIn(scope, SharingStarted.Eagerly, TelemetryConsent.UNANSWERED)

    private val undecided = Channel<AnalyticsEvent>(UNDECIDED_CAPACITY, BufferOverflow.DROP_OLDEST)

    init {
        scope.launch {
            for (event in undecided) {
                if (consent.first { it.isAnswered }.isShared && isSinkReady()) send(event)
            }
        }
    }

    override fun track(event: AnalyticsEvent) {
        if (buildInfo.isDeveloperBuild) return
        when (consent.value) {
            TelemetryConsent.SHARED -> if (sink.isReady()) send(event) else undecided.trySend(event)
            TelemetryConsent.DECLINED -> Unit
            TelemetryConsent.UNANSWERED -> undecided.trySend(event)
        }
    }

    private suspend fun isSinkReady(): Boolean {
        repeat(READY_CHECKS) {
            if (sink.isReady()) return true
            delay(READY_CHECK_MS)
        }
        return sink.isReady()
    }

    private fun send(event: AnalyticsEvent) {
        sink.log(event.name, event.params + (PLATFORM_PARAM to buildInfo.platform))
    }

    private companion object {
        const val PLATFORM_PARAM = "platform"
        const val UNDECIDED_CAPACITY = 64
        const val READY_CHECKS = 40
        const val READY_CHECK_MS = 250L
    }
}
