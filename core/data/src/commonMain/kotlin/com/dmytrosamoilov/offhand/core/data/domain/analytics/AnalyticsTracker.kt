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
interface AnalyticsSink {

    fun log(name: String, params: Map<String, Any>)
}
