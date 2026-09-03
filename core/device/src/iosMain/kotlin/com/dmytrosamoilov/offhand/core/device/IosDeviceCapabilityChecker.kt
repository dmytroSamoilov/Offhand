package com.dmytrosamoilov.offhand.core.device

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.Foundation.NSPageSize
import platform.Foundation.NSProcessInfo
import platform.darwin.HOST_VM_INFO64
import platform.darwin.HOST_VM_INFO64_COUNT
import platform.darwin.KERN_SUCCESS
import platform.darwin.host_statistics64
import platform.darwin.integer_tVar
import platform.darwin.mach_host_self
import platform.darwin.vm_statistics64_data_t

class IosDeviceCapabilityChecker : DeviceCapabilityChecker {

    @OptIn(ExperimentalForeignApi::class)
    override fun snapshot(): DeviceCapability {
        val processInfo = NSProcessInfo.processInfo
        return DeviceCapability(
            totalRamMb = (processInfo.physicalMemory / BYTES_PER_MB).toLong(),
            availableRamMb = availableRamMb(),
            cpuCores = processInfo.processorCount.toInt(),
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun availableRamMb(): Long = memScoped {
        val stats = alloc<vm_statistics64_data_t>()
        val count = alloc<UIntVar>()
        count.value = HOST_VM_INFO64_COUNT.convert()
        val result = host_statistics64(
            mach_host_self(),
            HOST_VM_INFO64,
            stats.ptr.reinterpret<integer_tVar>(),
            count.ptr,
        )
        if (result != KERN_SUCCESS) return@memScoped 0L
        val reclaimablePages = stats.free_count.toLong() + stats.inactive_count.toLong()
        reclaimablePages * NSPageSize().toLong() / BYTES_PER_MB.toLong()
    }

    private companion object {
        val BYTES_PER_MB: ULong = 1_048_576uL
    }
}
