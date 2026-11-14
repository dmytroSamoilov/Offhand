package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

// A file the decoder rejected never became a note, so a free user gets the
// import back.
class RefundFreeImportUseCase(
    private val userPreferences: UserPreferencesRepository,
    private val proStatusRepository: ProStatusRepository,
) {
    suspend operator fun invoke() {
        if (proStatusRepository.observeStatus().first().isPro) return
        userPreferences.refundFreeImport()
    }
}
