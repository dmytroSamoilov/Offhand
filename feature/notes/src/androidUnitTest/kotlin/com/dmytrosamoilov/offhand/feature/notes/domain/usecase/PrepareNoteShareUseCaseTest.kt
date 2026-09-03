package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.security.AudioInputStream
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrepareNoteShareUseCaseTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val audioStore: EncryptedAudioStore = mockk()
    private val labelsProvider: NoteShareLabelsProvider = mockk {
        every { labels() } returns NoteShareLabels(
            title = "Title",
            date = "Date",
            overview = "Overview",
            transcript = "Transcript",
        )
        every { fallbackTitle() } returns "Recording"
    }
    private val dateLabelFormatter: DateLabelFormatter = mockk {
        every { dateTime(any()) } returns "Jun 15, 2025 · 15:06"
    }

    private lateinit var useCase: PrepareNoteShareUseCase

    private val note = Note(
        id = 1,
        title = "Board meeting",
        body = "Budget approved.",
        transcript = "We approved the budget.",
        createdAtEpochMs = 1_750_000_000_000,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        audioFileName = "note-1.pcm.enc",
    )

    @Before
    fun setUp() {
        val cacheDirectoryProvider: ShareCacheDirectoryProvider = mockk {
            every { shareDirectoryPath() } returns folder.root.absolutePath
        }
        useCase = AndroidPrepareNoteShareUseCase(
            audioStore,
            labelsProvider,
            cacheDirectoryProvider,
            dateLabelFormatter,
        )
    }

    @Test
    fun `sharing the note only writes a single text file`() = runTest {
        val share = useCase(note, includeNote = true, includeAudio = false)

        assertEquals(1, share.filePaths.size)
        assertEquals("text/plain", share.mimeType)
        val content = File(share.filePaths.single()).readText()
        assertTrue(content.contains("Title: Board meeting"))
        assertTrue(content.contains("We approved the budget."))
    }

    @Test
    fun `sharing audio only writes a wav file`() = runTest {
        every { audioStore.openForRead("note-1.pcm.enc") } returns AudioInputStream(ByteArrayInputStream(ByteArray(10)))

        val share = useCase(note, includeNote = false, includeAudio = true)

        assertEquals(1, share.filePaths.size)
        assertEquals("audio/wav", share.mimeType)
        assertTrue(share.filePaths.single().endsWith(".wav"))
    }

    @Test
    fun `sharing note and audio produces two files with a generic mime type`() = runTest {
        every { audioStore.openForRead("note-1.pcm.enc") } returns AudioInputStream(ByteArrayInputStream(ByteArray(10)))

        val share = useCase(note, includeNote = true, includeAudio = true)

        assertEquals(2, share.filePaths.size)
        assertEquals("*/*", share.mimeType)
    }

    @Test
    fun `previous share directory contents are cleared before writing`() = runTest {
        val staleFile = File(folder.root, "stale.txt").apply { writeText("stale") }

        useCase(note, includeNote = true, includeAudio = false)

        assertTrue(!staleFile.exists())
    }
}
