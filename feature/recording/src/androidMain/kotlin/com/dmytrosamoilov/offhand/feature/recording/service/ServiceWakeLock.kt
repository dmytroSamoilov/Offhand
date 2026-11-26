package com.dmytrosamoilov.offhand.feature.recording.service

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Keeps the processor awake while the service records or works on a note, so
// a locked phone does not put the work to sleep. The lock is taken for a few
// minutes at a time and renewed, so one that is never released still ends.
class ServiceWakeLock(context: Context) {

    private val wakeLock: PowerManager.WakeLock? =
        context.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
            ?.apply { setReferenceCounted(false) }

    private var renewal: Job? = null

    fun holdWhile(scope: CoroutineScope) {
        if (renewal?.isActive == true) return
        renewal = scope.launch {
            while (isActive) {
                wakeLock?.acquire(HOLD_MS)
                delay(RENEW_EVERY_MS)
            }
        }
    }

    fun release() {
        renewal?.cancel()
        renewal = null
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private companion object {
        const val TAG = "Offhand:RecordingService"
        const val HOLD_MS = 10L * 60L * 1000L
        const val RENEW_EVERY_MS = 5L * 60L * 1000L
    }
}
