package com.dmytrosamoilov.offhand.core.device

import android.app.ActivityManager
import android.content.Context
import timber.log.Timber

class AndroidDeviceCapabilityChecker(
    private val context: Context,
) : DeviceCapabilityChecker {

    override fun snapshot(): DeviceCapability {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val capability = DeviceCapability(
            totalRamMb = memoryInfo.totalMem / BYTES_PER_MB,
            availableRamMb = memoryInfo.availMem / BYTES_PER_MB,
            cpuCores = Runtime.getRuntime().availableProcessors(),
        )
        Timber.tag(LOG_TAG).d(
            "snapshot=%s capable=%s (ram threshold=%dMB, cores threshold=%d)",
            capability,
            capability.isLocalLlmCapable(),
            MIN_TOTAL_RAM_MB,
            MIN_CPU_CORES,
        )
        return capability
    }

    private companion object {
        const val BYTES_PER_MB: Long = 1024L * 1024L
        const val LOG_TAG = "DeviceCapability"
    }
}
