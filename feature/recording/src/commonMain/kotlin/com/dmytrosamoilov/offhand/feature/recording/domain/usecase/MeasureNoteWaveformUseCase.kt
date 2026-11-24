package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.audio.WaveformAccumulator
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveform
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.closeQuietly
import com.dmytrosamoilov.offhand.core.security.readChunk
import kotlinx.coroutines.yield

class MeasureNoteWaveformUseCase(
    private val audioStore: EncryptedAudioStore,
    private val waveformRepository: NoteWaveformRepository,
) {
    suspend operator fun invoke(noteId: Long, audioFileName: String) {
        val totalBytes = audioStore.pcmSizeOf(audioFileName)
        if (totalBytes <= 0L) return
        val accumulator = WaveformAccumulator(totalBytes)
        readInto(accumulator, audioFileName)
        waveformRepository.saveWaveform(NoteWaveform(noteId = noteId, levels = accumulator.levels()))
    }

    private suspend fun readInto(accumulator: WaveformAccumulator, audioFileName: String) {
        val stream = audioStore.openForRead(audioFileName)
        val buffer = ByteArray(READ_BUFFER_BYTES)
        try {
            while (true) {
                val read = stream.readChunk(buffer)
                if (read <= 0) break
                accumulator.add(buffer, read)
                yield()
            }
        } finally {
            stream.closeQuietly()
        }
    }

    private companion object {
        const val READ_BUFFER_BYTES = 64 * 1024
    }
}
