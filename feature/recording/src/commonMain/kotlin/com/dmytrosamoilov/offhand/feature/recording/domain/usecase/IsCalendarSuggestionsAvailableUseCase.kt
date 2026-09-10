package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import kotlinx.coroutines.flow.first

class IsCalendarSuggestionsAvailableUseCase(
    private val repository: EntitlementsRepository,
) {
    suspend operator fun invoke(): Boolean = repository.observeEntitlements().first().calendarSuggestionsUnlocked
}
