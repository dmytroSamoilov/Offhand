package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource

// Staged copies live in the platform's cache until the decoder consumes them;
// this removes the ones that never get there, such as a declined share.
interface AudioImportStaging {

    suspend fun discard(sources: List<AudioImportSource>)
}
