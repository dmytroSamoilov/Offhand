package com.dmytrosamoilov.offhand.feature.paywall.presentation

import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsage
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.GetProTrialDaysLeftUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.GetProTrialRecapUseCase
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
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
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
    private val getProTrialRecap: GetProTrialRecapUseCase = mockk()
    private val getProTrialDaysLeft: GetProTrialDaysLeftUseCase = mockk {
        coEvery { this@mockk.invoke() } returns 9
    }
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
    fun `the paywall after the trial shows what was used and still offers the plans`() = runTest {
        val recap = listOf(ProTrialUsageCount(ProTrialUsage.MEETING_NOTES, 3), ProTrialUsageCount(ProTrialUsage.PDF_EXPORTS, 2))
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.TRIAL_ENDED)
        coEvery { getProTrialRecap() } returns recap
        val viewModel = createViewModel()

        viewModel.onOpened()

        assertEquals(recap, viewModel.uiState.value.trialRecap)
        assertEquals(PaywallMode.OFFER, viewModel.uiState.value.mode)
    }

    @Test
    fun `someone on the free trial is shown the offer and no recap`() = runTest {
        status.value = ProStatus.trial(endsAtMs = 1L)
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.FREE_TRIAL)
        val viewModel = createViewModel()

        viewModel.onOpened()

        assertEquals(PaywallMode.OFFER, viewModel.uiState.value.mode)
        assertEquals(emptyList<ProTrialUsageCount>(), viewModel.uiState.value.trialRecap)
        assertEquals(9, viewModel.uiState.value.trialDaysLeft)
    }

    @Test
    fun `opening picks up the feature that asked for the paywall`() = runTest {
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.DOCUMENT_EXPORT)
        val viewModel = createViewModel()

        viewModel.onOpened()

        assertEquals(ProFeature.DOCUMENT_EXPORT, viewModel.uiState.value.feature)
    }

    @Test
    fun `opening as a subscriber shows the benefits and loads no offers`() = runTest {
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.GENERAL)
        status.value = ProStatus.LIFETIME
        val viewModel = createViewModel()

        viewModel.onOpened()

        assertEquals(PaywallMode.BENEFITS, viewModel.uiState.value.mode)
        coVerify(exactly = 1) { loadProOffers() }
    }

    @Test
    fun `opening as a free user shows the paywall`() = runTest {
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.GENERAL)
        val viewModel = createViewModel()

        viewModel.onOpened()

        assertEquals(PaywallMode.OFFER, viewModel.uiState.value.mode)
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
    fun `a purchase that does not go through is reported by how it ended`() = runTest {
        val tracker: AnalyticsTracker = mockk(relaxed = true)
        val viewModel = createViewModel(tracker)

        coEvery { purchasePro(ProPlan.YEARLY) } returns PurchaseOutcome.CANCELLED
        viewModel.onPurchaseClicked()
        coEvery { purchasePro(ProPlan.YEARLY) } returns PurchaseOutcome.FAILED
        viewModel.onPurchaseClicked()

        verify(exactly = 1) { tracker.track(match { it.name == "purchase_cancelled" }) }
        verify(exactly = 1) { tracker.track(match { it.name == "purchase_failed" }) }
        verify(exactly = 0) { tracker.track(match { it.name == "purchase_completed" }) }
    }

    @Test
    fun `keeping the free trial is reported once and not as a dismissal`() = runTest {
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.FREE_TRIAL)
        val tracker: AnalyticsTracker = mockk(relaxed = true)
        val viewModel = createViewModel(tracker)
        viewModel.onOpened()

        viewModel.onKeepTrialClicked()

        verify(exactly = 1) { tracker.track(match { it.name == "trial_offer_kept" }) }
        verify(exactly = 0) { tracker.track(match { it.name == "paywall_dismissed" }) }
        verify { gate.onPaywallClosed() }
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

    @Test
    fun `one presentation reports its dismissal once however many controls close it`() = runTest {
        every { gate.requestedFeature } returns MutableStateFlow(ProFeature.GENERAL)
        val tracker: AnalyticsTracker = mockk(relaxed = true)
        val viewModel = createViewModel(tracker)
        viewModel.onOpened()

        viewModel.onClosed()
        viewModel.onClosed()

        verify(exactly = 1) { tracker.track(match { it.name == "paywall_dismissed" }) }
        verify(exactly = 2) { gate.onPaywallClosed() }
    }

    private fun createViewModel(tracker: AnalyticsTracker = mockk(relaxed = true)) = PaywallViewModel(
        loadProOffers = loadProOffers,
        purchasePro = purchasePro,
        restoreProPurchases = restoreProPurchases,
        observeProStatus = observeProStatus,
        getProTrialRecap = getProTrialRecap,
        getProTrialDaysLeft = getProTrialDaysLeft,
        isSummaryAvailable = mockk { every { this@mockk.invoke() } returns true },
        proUpgradeGate = gate,
        analyticsTracker = tracker,
    )
}
