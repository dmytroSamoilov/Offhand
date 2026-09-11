package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GetNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
    private val entitlements: ProStatusRepository,
) {
    suspend operator fun invoke(): NoteStyleRef {
        val style = repository.preferences.first().noteStyle
        if (style is NoteStyleRef.BuiltIn) return style
        val unlocked = entitlements.observeStatus().first().isPro
        return if (unlocked) style else NoteStyleRef.DEFAULT
    }
}
