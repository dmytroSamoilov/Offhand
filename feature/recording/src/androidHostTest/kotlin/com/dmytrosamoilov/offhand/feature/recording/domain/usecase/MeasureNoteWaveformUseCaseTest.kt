package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.audio.WaveformAccumulator
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveform
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import com.dmytrosamoilov.offhand.core.security.AudioInputStream
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasureNoteWaveformUseCaseTest {

    private val audioStore: EncryptedAudioStore = mockk()
    private val waveformRepository: NoteWaveformRepository = mockk(relaxed = true)
    private val useCase = MeasureNoteWaveformUseCase(audioStore, waveformRepository)

    @Test
    fun `saves one level per section, loud where the recording is loud`() = runTest {
        val pcm = pcmOf(List(4_000) { 9_000 } + List(4_000) { 0 })
        givenAudio(pcm)
        val saved = slot<NoteWaveform>()
        coEvery { waveformRepository.saveWaveform(capture(saved)) } returns Unit

        useCase(noteId = 7, audioFileName = FILE)

        assertEquals(7L, saved.captured.noteId)
        assertEquals(WaveformAccumulator.SECTIONS, saved.captured.levels.size)
        assertEquals(100, saved.captured.levels.first())
        assertEquals(0, saved.captured.levels.last())
    }

    @Test
    fun `saves nothing for an empty recording`() = runTest {
        every { audioStore.pcmSizeOf(FILE) } returns 0L

        useCase(noteId = 7, audioFileName = FILE)

        coVerify(exactly = 0) { waveformRepository.saveWaveform(any()) }
    }

    private fun givenAudio(pcm: ByteArray) {
        every { audioStore.pcmSizeOf(FILE) } returns pcm.size.toLong()
        every { audioStore.openForRead(FILE) } returns AudioInputStream(ByteArrayInputStream(pcm))
    }

    private fun pcmOf(samples: List<Int>): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { index, sample ->
            bytes[index * 2] = (sample and 0xFF).toByte()
            bytes[index * 2 + 1] = (sample shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    private companion object {
        const val FILE = "note.pcm.enc"
    }
}
