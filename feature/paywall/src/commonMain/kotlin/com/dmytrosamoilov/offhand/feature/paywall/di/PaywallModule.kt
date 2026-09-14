package com.dmytrosamoilov.offhand.feature.paywall.di

import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.LoadProOffersUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.PurchaseProUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.RestoreProPurchasesUseCase
import com.dmytrosamoilov.offhand.feature.paywall.presentation.PaywallViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featurePaywallModule = module {
    factoryOf(::LoadProOffersUseCase)
    factoryOf(::ObserveProStatusUseCase)
    factoryOf(::PurchaseProUseCase)
    factoryOf(::RestoreProPurchasesUseCase)
    viewModelOf(::PaywallViewModel)
}
