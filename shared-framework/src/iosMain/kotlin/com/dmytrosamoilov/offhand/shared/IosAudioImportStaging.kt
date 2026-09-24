@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportStaging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager

class IosAudioImportStaging : AudioImportStaging {

    override suspend fun discard(sources: List<AudioImportSource>) = withContext(Dispatchers.Default) {
        sources.forEach { source -> NSFileManager.defaultManager.removeItemAtPath(source.handle, error = null) }
    }
}
