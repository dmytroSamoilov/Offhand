package com.dmytrosamoilov.offhand.feature.paywall.di

import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.AcknowledgeProTrialNoticeUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.GetProTrialRecapUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.KeepProTrialInSyncUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.LoadProOffersUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.PurchaseProUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.RestoreProPurchasesUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.SettleProTrialUseCase
import com.dmytrosamoilov.offhand.feature.paywall.presentation.PaywallViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featurePaywallModule = module {
    factoryOf(::LoadProOffersUseCase)
    factoryOf(::ObserveProStatusUseCase)
    factoryOf(::PurchaseProUseCase)
    factoryOf(::RestoreProPurchasesUseCase)
    factory { SettleProTrialUseCase(get(), get(), get()) }
    factoryOf(::AcknowledgeProTrialNoticeUseCase)
    factoryOf(::KeepProTrialInSyncUseCase)
    factoryOf(::GetProTrialRecapUseCase)
    viewModelOf(::PaywallViewModel)
}
