package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

data class Entitlements(
    val customStylesUnlocked: Boolean,
    val audioImportUnlocked: Boolean,
    val calendarSuggestionsUnlocked: Boolean,
)

interface EntitlementsRepository {

    fun observeEntitlements(): Flow<Entitlements>
}
