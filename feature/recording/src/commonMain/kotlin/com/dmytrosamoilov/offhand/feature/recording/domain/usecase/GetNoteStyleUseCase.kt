package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GetNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
    private val entitlements: EntitlementsRepository,
) {
    suspend operator fun invoke(): NoteStyleRef {
        val style = repository.preferences.first().noteStyle
        if (style is NoteStyleRef.BuiltIn) return style
        val unlocked = entitlements.observeEntitlements().first().customStylesUnlocked
        return if (unlocked) style else NoteStyleRef.DEFAULT
    }
}
