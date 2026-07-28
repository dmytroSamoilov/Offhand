package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.MIN_CPU_CORES
import com.dmytrosamoilov.offhand.core.device.MIN_TOTAL_RAM_MB
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import java.util.Locale

private const val MB_PER_GB = 1024f
private const val BYTES_PER_GB = 1024f * 1024f * 1024f

internal fun DeviceCapability.toDeviceSpecsUi(): DeviceSpecsUi = DeviceSpecsUi(
    totalRamGb = formatGb(totalRamMb),
    requiredRamGb = formatGb(MIN_TOTAL_RAM_MB),
    isRamSatisfied = totalRamMb >= MIN_TOTAL_RAM_MB,
    cpuCores = cpuCores,
    requiredCpuCores = MIN_CPU_CORES,
    isCoresSatisfied = cpuCores >= MIN_CPU_CORES,
)

internal fun NotePresetOption.toDomain(): NotePreset = when (this) {
    NotePresetOption.SUMMARY -> NotePreset.SUMMARY
    NotePresetOption.MEETING -> NotePreset.MEETING
    NotePresetOption.VISIT -> NotePreset.VISIT
    NotePresetOption.LEGAL -> NotePreset.LEGAL
}

internal fun formatDownloadSizeGb(bytes: Long): String =
    String.format(Locale.US, "%.1f", bytes / BYTES_PER_GB)

private fun formatGb(ramMb: Long): String = String.format(Locale.US, "%.1f", ramMb / MB_PER_GB)
