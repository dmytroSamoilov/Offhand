package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsSink

// Implemented in Swift over FirebaseAnalytics, same bridge pattern as the
// backup crypto; the Swift side checks that Firebase is configured.
interface IosAnalyticsBridge {

    fun logEvent(name: String, params: Map<String, Any>)
}

internal class IosAnalyticsSink(
    private val bridge: IosAnalyticsBridge,
) : AnalyticsSink {

    override fun log(name: String, params: Map<String, Any>) = bridge.logEvent(name, params)
}
