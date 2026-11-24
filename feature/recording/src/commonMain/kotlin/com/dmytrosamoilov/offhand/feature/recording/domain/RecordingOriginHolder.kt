package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.analytics.RecordingOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

// Remembers what asked for the next recording (the widget, a shortcut, the
// onboarding button) until the record sheet starts it. A recording nobody
// announced was started in the app.
class RecordingOriginHolder {

    private val expected = MutableStateFlow(RecordingOrigin.APP)

    fun expect(origin: RecordingOrigin) {
        expected.value = origin
    }

    fun take(): RecordingOrigin = expected.getAndUpdate { RecordingOrigin.APP }

    fun clear() {
        expected.value = RecordingOrigin.APP
    }
}
