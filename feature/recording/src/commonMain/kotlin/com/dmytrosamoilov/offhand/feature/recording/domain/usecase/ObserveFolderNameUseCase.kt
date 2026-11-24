package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObserveFolderNameUseCase(
    private val foldersRepository: FoldersRepository,
) {
    operator fun invoke(folderId: Long?): Flow<String?> {
        if (folderId == null) return flowOf(null)
        return foldersRepository.observeFolders()
            .map { folders -> folders.firstOrNull { it.id == folderId }?.name }
            .distinctUntilChanged()
    }
}
