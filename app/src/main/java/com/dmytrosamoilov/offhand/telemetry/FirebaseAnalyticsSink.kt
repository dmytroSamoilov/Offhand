package com.dmytrosamoilov.offhand.telemetry

import android.content.Context
import androidx.core.os.bundleOf
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsSink
import com.google.firebase.analytics.FirebaseAnalytics

// Only reachable behind an initialised FirebaseApp: getInstance would otherwise
// self-initialise measurement before the user has agreed to anything.
class FirebaseAnalyticsSink(
    private val context: Context,
) : AnalyticsSink {

    override fun log(name: String, params: Map<String, Any>) {
        FirebaseReporting.instanceOrNull(context) ?: return
        FirebaseAnalytics.getInstance(context).logEvent(name, bundleOf(*params.toList().toTypedArray()))
    }
}
