package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.audio.PcmAudioPlayer
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkReviewAttemptUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.PrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ShouldRequestReviewUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val recordingProcessController: RecordingProcessController,
    private val dateLabelFormatter: DateLabelFormatter,
    observeNotes: ObserveNotesUseCase,
    observeDeveloperOptions: ObserveDeveloperOptionsUseCase,
    private val getNote: GetNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val prepareNoteShare: PrepareNoteShareUseCase,
    private val shouldRequestReview: ShouldRequestReviewUseCase,
    private val markReviewAttempt: MarkReviewAttemptUseCase,
    val reviewLauncher: InAppReviewLauncher,
    private val audioPlayer: PcmAudioPlayer,
    private val audioStore: EncryptedAudioStore,
    sessionManager: RecordingSessionManager,
    aiCoreDownloadStatus: AiCoreDownloadStatus,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = mutableUiState.asStateFlow()

    private val mutableReviewRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reviewRequests: SharedFlow<Unit> = mutableReviewRequests.asSharedFlow()

    private var selectedNote: Note? = null

    init {
        viewModelScope.launch {
            observeNotes().collect { notes ->
                mutableUiState.update { it.copy(sections = notes.toSectionsUi(dateLabelFormatter)) }
                refreshSelected(notes)
            }
        }
        viewModelScope.launch {
            audioPlayer.state.collect { playback ->
                mutableUiState.update { it.copy(playback = playback.toUi()) }
            }
        }
        viewModelScope.launch {
            observeDeveloperOptions().collect { enabled ->
                mutableUiState.update { it.copy(isDeveloperMode = enabled) }
            }
        }
        viewModelScope.launch {
            sessionManager.noteProgress.collect { progress ->
                mutableUiState.update { it.copy(noteProgress = progress) }
            }
        }
        viewModelScope.launch {
            aiCoreDownloadStatus.state.collect { downloadState ->
                mutableUiState.update { it.copy(modelPreparation = downloadState.toPreparationUi()) }
            }
        }
    }

    fun onNoteSelected(id: Long) {
        launchSafely(showLoading = false) {
            val note = getNote(id) ?: return@launchSafely
            selectedNote = note
            loadAudio(note)
            mutableUiState.update {
                it.copy(
                    selected = note.toDetailUi(dateLabelFormatter),
                    editor = null,
                    isDeleteConfirmationVisible = false,
                )
            }
            maybeRequestReview(note)
        }
    }

    private suspend fun maybeRequestReview(note: Note) {
        if (note.status == NoteStatus.READY && shouldRequestReview()) {
            mutableReviewRequests.emit(Unit)
        }
    }

    fun onReviewAttemptSucceeded() {
        launchSafely(showLoading = false) {
            markReviewAttempt()
        }
    }

    private fun refreshSelected(notes: List<Note>) {
        val current = selectedNote ?: return
        val refreshed = notes.firstOrNull { it.id == current.id }
        if (refreshed == null || refreshed == current) return
        selectedNote = refreshed
        mutableUiState.update { state ->
            if (state.editor != null) {
                state
            } else {
                state.copy(selected = refreshed.toDetailUi(dateLabelFormatter))
            }
        }
    }

    fun onDetailClosed() {
        selectedNote = null
        audioPlayer.reset()
        mutableUiState.update {
            it.copy(
                selected = null,
                editor = null,
                isDeleteConfirmationVisible = false,
                isShareDialogVisible = false,
                isPresetSheetVisible = false,
                pendingShare = null,
            )
        }
    }

    fun onPresetSheetRequested() {
        mutableUiState.update { it.copy(isPresetSheetVisible = true) }
    }

    fun onPresetSheetDismissed() {
        mutableUiState.update { it.copy(isPresetSheetVisible = false) }
    }

    fun onPresetSelected(preset: NotePreset) {
        val note = selectedNote ?: return
        mutableUiState.update { it.copy(isPresetSheetVisible = false) }
        recordingProcessController.restructureNote(note.id, preset)
    }

    fun onShareRequested() {
        mutableUiState.update { it.copy(isShareDialogVisible = true) }
    }

    fun onShareDismissed() {
        mutableUiState.update { it.copy(isShareDialogVisible = false) }
    }

    fun onShareConfirmed(includeNote: Boolean, includeAudio: Boolean) {
        val note = selectedNote ?: return
        if (!includeNote && !includeAudio) return
        mutableUiState.update { it.copy(isShareDialogVisible = false) }
        launchSafely {
            val share = prepareNoteShare(note, includeNote, includeAudio)
            mutableUiState.update { it.copy(pendingShare = share.toUi()) }
        }
    }

    fun onShareLaunched() {
        mutableUiState.update { it.copy(pendingShare = null) }
    }

    fun onPlayPauseClicked() {
        if (mutableUiState.value.playback.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }

    fun onSeekRequested(fraction: Float) {
        val durationMs = audioPlayer.state.value.durationMs
        audioPlayer.seekTo((durationMs * fraction).toLong())
    }

    private fun loadAudio(note: Note) {
        val fileName = note.audioFileName
        if (fileName != null) {
            audioPlayer.load { audioStore.openSeekableForRead(fileName) }
        } else {
            audioPlayer.reset()
        }
    }

    fun onEditStarted() {
        val selected = mutableUiState.value.selected ?: return
        mutableUiState.update {
            it.copy(editor = NoteEditorUi(title = selected.title, transcript = selected.transcript))
        }
    }

    fun onEditorTitleChanged(title: String) {
        mutableUiState.update { state ->
            state.copy(editor = state.editor?.copy(title = title))
        }
    }

    fun onEditorTranscriptChanged(transcript: String) {
        mutableUiState.update { state ->
            state.copy(editor = state.editor?.copy(transcript = transcript))
        }
    }

    fun onEditCancelled() {
        mutableUiState.update { it.copy(editor = null) }
    }

    fun onEditSaved() {
        val note = selectedNote ?: return
        val editor = mutableUiState.value.editor ?: return
        launchSafely {
            val updated = note.copy(
                title = editor.title.trim(),
                transcript = editor.transcript.trim(),
            )
            updateNote(updated)
            selectedNote = updated
            mutableUiState.update {
                it.copy(selected = updated.toDetailUi(dateLabelFormatter), editor = null)
            }
        }
    }

    fun onRetryTranscriptionRequested() {
        val note = selectedNote ?: return
        val audioFileName = note.audioFileName ?: return
        recordingProcessController.retryNote(note.id, audioFileName)
    }

    fun onRetranscribeRequested() {
        mutableUiState.update { it.copy(isRetranscribeConfirmationVisible = true) }
    }

    fun onRetranscribeDismissed() {
        mutableUiState.update { it.copy(isRetranscribeConfirmationVisible = false) }
    }

    fun onRetranscribeConfirmed() {
        mutableUiState.update { it.copy(isRetranscribeConfirmationVisible = false) }
        onRetryTranscriptionRequested()
    }

    fun onDeleteRequested() {
        mutableUiState.update { it.copy(isDeleteConfirmationVisible = true) }
    }

    fun onDeleteDismissed() {
        mutableUiState.update { it.copy(isDeleteConfirmationVisible = false) }
    }

    fun onDeleteConfirmed() {
        val selected = mutableUiState.value.selected ?: return
        launchSafely {
            audioPlayer.reset()
            deleteNote(selected.id)
            selectedNote = null
            mutableUiState.update {
                it.copy(
                    selected = null,
                    editor = null,
                    isDeleteConfirmationVisible = false,
                )
            }
        }
    }

    override fun onCleared() {
        audioPlayer.release()
        super.onCleared()
    }
}
