package com.dmytrosamoilov.offhand.core.data.domain.analytics

// Parameter values are strings or longs, the two types Firebase aggregates.
data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
)

interface AnalyticsTracker {

    fun track(event: AnalyticsEvent)
}

// The platform end: hands an event to Firebase once consent has been checked.
// Firebase is switched on a moment after the consent is stored, so the sink
// says when it can take an event; one logged earlier would be lost.
interface AnalyticsSink {

    fun isReady(): Boolean = true

    fun log(name: String, params: Map<String, Any>)
}
