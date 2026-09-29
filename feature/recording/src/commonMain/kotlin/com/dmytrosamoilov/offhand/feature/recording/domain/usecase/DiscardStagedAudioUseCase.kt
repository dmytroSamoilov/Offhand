package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportStaging

class DiscardStagedAudioUseCase(
    private val staging: AudioImportStaging,
) {
    suspend operator fun invoke(sources: List<AudioImportSource>) {
        if (sources.isEmpty()) return
        staging.discard(sources)
    }
}
