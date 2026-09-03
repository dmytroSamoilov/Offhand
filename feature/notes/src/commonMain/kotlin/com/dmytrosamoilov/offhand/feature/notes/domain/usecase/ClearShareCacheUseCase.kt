package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider

class ClearShareCacheUseCase(
    private val shareCacheDirectoryProvider: ShareCacheDirectoryProvider,
) {

    suspend operator fun invoke() {
        shareCacheDirectoryProvider.clearShareDirectory()
    }
}
