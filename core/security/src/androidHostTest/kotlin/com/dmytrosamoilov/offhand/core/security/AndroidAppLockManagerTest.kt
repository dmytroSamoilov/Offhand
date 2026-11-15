package com.dmytrosamoilov.offhand.core.security

import android.app.KeyguardManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidAppLockManagerTest {

    private val keyguard: KeyguardManager = mockk()
    private val context: Context = mockk {
        every { getSystemService(Context.KEYGUARD_SERVICE) } returns keyguard
    }

    @Test
    fun `stays unlocked when the device has no credential`() {
        every { keyguard.isDeviceSecure } returns false
        val manager = AndroidAppLockManager(context)

        manager.markLocked()

        assertEquals(AppLockState.UNLOCKED, manager.lockState.value)
    }

    @Test
    fun `locks again when the device has a credential`() {
        every { keyguard.isDeviceSecure } returns true
        val manager = AndroidAppLockManager(context)
        manager.markUnlocked()

        manager.markLocked()

        assertEquals(AppLockState.LOCKED, manager.lockState.value)
    }

    @Test
    fun `stops locking once the credential is removed`() {
        every { keyguard.isDeviceSecure } returns true
        val manager = AndroidAppLockManager(context)
        manager.markUnlocked()
        every { keyguard.isDeviceSecure } returns false

        manager.markLocked()

        assertEquals(AppLockState.UNLOCKED, manager.lockState.value)
    }
}
