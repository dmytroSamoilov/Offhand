package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProUpgradeCoordinatorTest {

    private val status = MutableStateFlow(ProStatus.LIFETIME)
    private val coordinator = ProUpgradeCoordinator(FakeProStatusRepository(status))

    @Test
    fun `a subscriber passes the gate without a paywall`() = runTest {
        assertTrue(coordinator.requirePro(ProFeature.DOCUMENT_EXPORT))
        assertNull(coordinator.requestedFeature.value)
    }

    @Test
    fun `showing the benefits opens the paywall for a subscriber too`() {
        coordinator.showBenefits()

        assertEquals(ProFeature.GENERAL, coordinator.requestedFeature.value)
        coordinator.onPaywallClosed()
        assertNull(coordinator.requestedFeature.value)
    }

    private class FakeProStatusRepository(private val status: Flow<ProStatus>) : ProStatusRepository {
        override fun observeStatus(): Flow<ProStatus> = status
    }
}
