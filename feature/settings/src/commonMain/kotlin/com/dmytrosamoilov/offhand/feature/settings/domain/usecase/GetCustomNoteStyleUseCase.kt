package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository

class GetCustomNoteStyleUseCase(
    private val repository: CustomNoteStylesRepository,
) {
    suspend operator fun invoke(id: Long): CustomNoteStyle? = repository.getStyle(id)
}
