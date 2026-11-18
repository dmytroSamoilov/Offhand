package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.audio.SilenceDetector
import com.dmytrosamoilov.offhand.core.audio.SilenceSkip
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.closeQuietly
import com.dmytrosamoilov.offhand.core.security.readChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class FindSilenceUseCase(
    private val audioStore: EncryptedAudioStore,
) {
    suspend operator fun invoke(audioFileName: String): List<SilenceSkip> = withContext(Dispatchers.IO) {
        val detector = SilenceDetector()
        val stream = audioStore.openForRead(audioFileName)
        val buffer = ByteArray(READ_BUFFER_BYTES)
        try {
            while (true) {
                val read = stream.readChunk(buffer)
                if (read <= 0) break
                detector.add(buffer, read)
                yield()
            }
        } finally {
            stream.closeQuietly()
        }
        detector.skips()
    }

    private companion object {
        const val READ_BUFFER_BYTES = 64 * 1024
    }
}
