package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.audio.PcmAudioPlayer
import com.dmytrosamoilov.offhand.core.audio.PcmPlaybackState
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.PrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val note = Note(
        id = 5,
        title = "Board meeting",
        body = "# Agenda\n- **Budget** approved\n- Hiring plan",
        transcript = "We approved the budget and discussed the hiring plan.",
        createdAtEpochMs = 1_750_000_000_000,
        transcriptionTimeMs = 12_400,
        structuringTimeMs = 4_200,
        hardwareBackend = "CPU",
    )

    private val observeNotes: ObserveNotesUseCase = mockk()
    private val observeDeveloperOptions: ObserveDeveloperOptionsUseCase = mockk {
        every { this@mockk() } returns flowOf(false)
    }
    private val getNote: GetNoteUseCase = mockk()
    private val updateNote: UpdateNoteUseCase = mockk(relaxed = true)
    private val deleteNote: DeleteNoteUseCase = mockk(relaxed = true)
    private val prepareNoteShare: PrepareNoteShareUseCase = mockk()
    private val audioPlayer: PcmAudioPlayer = mockk(relaxed = true) {
        every { state } returns MutableStateFlow(PcmPlaybackState())
    }
    private val audioStore: EncryptedAudioStore = mockk(relaxed = true)
    private val sessionManager: RecordingSessionManager = mockk {
        every { noteProgress } returns MutableStateFlow(emptyMap())
    }
    private val modelState = MutableStateFlow<ModelState>(ModelState.Ready)
    private val modelManager: ModelManager = mockk {
        every { this@mockk.modelState } returns this@NotesViewModelTest.modelState
        every { model } returns mockk<AvailableModel> {
            every { sizeInBytes } returns 3_000L
        }
        every { speechModelSizeInBytes } returns 1_000L
    }
    private val speechDownloadState =
        MutableStateFlow<SpeechModelState>(SpeechModelState.Downloaded)
    private val speechToText: SpeechToText = mockk {
        every { downloadState } returns speechDownloadState
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeNotes() } returns flowOf(listOf(note))
        coEvery { getNote(5) } returns note
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NotesViewModel(
        context = mockk(relaxed = true),
        observeNotes = observeNotes,
        observeDeveloperOptions = observeDeveloperOptions,
        getNote = getNote,
        updateNote = updateNote,
        deleteNote = deleteNote,
        prepareNoteShare = prepareNoteShare,
        audioPlayer = audioPlayer,
        audioStore = audioStore,
        sessionManager = sessionManager,
        aiCoreDownloadStatus = AiCoreDownloadStatus(modelManager, speechToText),
    )

    @Test
    fun `notes map to cards with markdown-free preview`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val section = viewModel.uiState.value.sections.single()
        val card = section.notes.single()
        assertEquals(5, card.id)
        assertEquals("Board meeting", card.title)
        assertFalse(card.preview.contains("#"))
        assertFalse(card.preview.contains("**"))
        assertTrue(card.preview.contains("Budget approved"))
        assertTrue(card.time.isNotBlank())
    }

    @Test
    fun `selecting a note loads detail with metrics`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onNoteSelected(5)
        dispatcher.scheduler.advanceUntilIdle()

        val detail = viewModel.uiState.value.selected
        assertEquals("Board meeting", detail?.title)
        assertEquals(9, detail?.wordCount)
        assertEquals("12.4 s", detail?.metrics?.transcriptionTime)
        assertEquals("4.2 s", detail?.metrics?.structuringTime)
        assertEquals("CPU", detail?.metrics?.hardwareBackend)
    }

    @Test
    fun `edit flow saves updated title and transcript`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onNoteSelected(5)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onEditStarted()
        viewModel.onEditorTitleChanged("Q3 board meeting")
        viewModel.onEditorTranscriptChanged("Updated transcript")
        viewModel.onEditSaved()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify {
            updateNote(
                note.copy(title = "Q3 board meeting", transcript = "Updated transcript"),
            )
        }
        val state = viewModel.uiState.value
        assertNull(state.editor)
        assertEquals("Q3 board meeting", state.selected?.title)
    }

    @Test
    fun `cancel edit discards editor without saving`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onNoteSelected(5)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onEditStarted()
        viewModel.onEditorTitleChanged("Discarded")
        viewModel.onEditCancelled()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { updateNote(any()) }
        assertNull(viewModel.uiState.value.editor)
        assertEquals("Board meeting", viewModel.uiState.value.selected?.title)
    }

    @Test
    fun `banner shows total progress across whisper and main model`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.modelPreparation)

        speechDownloadState.value = SpeechModelState.Downloading(
            bytesDownloaded = 500,
            bytesTotal = 1_000,
        )
        modelState.value = ModelState.NotDownloaded
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(12, viewModel.uiState.value.modelPreparation?.progressPercent)

        speechDownloadState.value = SpeechModelState.Downloaded
        modelState.value = ModelState.Downloading(
            progress = 0.5f,
            bytesDownloaded = 1_500,
            bytesTotal = 3_000,
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(62, viewModel.uiState.value.modelPreparation?.progressPercent)

        modelState.value = ModelState.Ready
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.modelPreparation)
    }

    @Test
    fun `confirmed delete removes note and clears selection`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onNoteSelected(5)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteRequested()
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
        viewModel.onDeleteConfirmed()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteNote(5) }
        val state = viewModel.uiState.value
        assertNull(state.selected)
        assertFalse(state.isDeleteConfirmationVisible)
    }
}
