package com.dmytrosamoilov.offhand.feature.paywall.presentation

import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.LoadProOffersUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.PurchaseProUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.RestoreProPurchasesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val status = MutableStateFlow(ProStatus.FREE)
    private val loadProOffers: LoadProOffersUseCase = mockk()
    private val purchasePro: PurchaseProUseCase = mockk()
    private val restoreProPurchases: RestoreProPurchasesUseCase = mockk()
    private val observeProStatus: ObserveProStatusUseCase = mockk()
    private val gate: ProUpgradeGate = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeProStatus() } returns status
        coEvery { loadProOffers() } returns listOf(
            ProOffer(ProPlan.YEARLY, "$24.99", trialDays = 14, priceMicros = 24_990_000, monthlyPrice = "$2.08"),
            ProOffer(ProPlan.LIFETIME, "$59.99", priceMicros = 59_990_000),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads offers with yearly preselected`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingOffers)
        assertEquals(ProPlan.YEARLY, state.selectedPlan)
        assertEquals(14, state.selectedOffer?.trialDays)
        assertEquals("$2.08", state.selectedOffer?.monthlyPrice)
        assertEquals(3, state.offers.last().yearsOfYearly)
    }

    @Test
    fun `opening picks up the feature that asked for the paywall`() = runTest {
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.DOCUMENT_EXPORT)
        val viewModel = createViewModel()

        viewModel.onOpened()

        assertEquals(ProFeature.DOCUMENT_EXPORT, viewModel.uiState.value.feature)
    }

    @Test
    fun `purchase reflects the store status and keeps no message`() = runTest {
        coEvery { purchasePro(ProPlan.LIFETIME) } coAnswers {
            status.value = ProStatus.LIFETIME
            PurchaseOutcome.PURCHASED
        }
        val viewModel = createViewModel()

        viewModel.onPlanSelected(ProPlan.LIFETIME)
        viewModel.onPurchaseClicked()

        assertTrue(viewModel.uiState.value.isPro)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `failed purchase shows a message and stays free`() = runTest {
        coEvery { purchasePro(ProPlan.YEARLY) } returns PurchaseOutcome.FAILED
        val viewModel = createViewModel()

        viewModel.onPurchaseClicked()

        assertEquals(PaywallMessageUi.Failed, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isPro)
    }

    @Test
    fun `restore without a purchase reports nothing to restore`() = runTest {
        coEvery { restoreProPurchases() } returns ProStatus.FREE
        val viewModel = createViewModel()

        viewModel.onRestoreClicked()

        assertEquals(PaywallMessageUi.NothingToRestore, viewModel.uiState.value.message)
        coVerify { restoreProPurchases() }
    }

    @Test
    fun `closing releases the gate`() = runTest {
        val viewModel = createViewModel()

        viewModel.onClosed()

        verify { gate.onPaywallClosed() }
    }

    private fun createViewModel() = PaywallViewModel(
        loadProOffers = loadProOffers,
        purchasePro = purchasePro,
        restoreProPurchases = restoreProPurchases,
        observeProStatus = observeProStatus,
        proUpgradeGate = gate,
    )
}
