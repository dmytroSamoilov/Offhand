package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadStatus
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestions
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.SuggestionStatus
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.notes.domain.AudioPlayer
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.CreateFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.FolderSaveResult
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MoveNoteToFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveFoldersUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.RenameFolderUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DeleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsCalendarSuggestionsAvailableUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateSuggestionStatusUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsDocumentExportAvailableUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.IsSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteExportFormat
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkReviewAttemptUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ObserveNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.PrepareNoteShareUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.SearchNotesUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ShouldRequestReviewUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.UpdateNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.ImportRejection
import com.dmytrosamoilov.offhand.feature.recording.domain.NoteProcessingEvent
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RequestNoteSuggestionsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val recordingProcessController: RecordingProcessController,
    private val dateLabelFormatter: DateLabelFormatter,
    observeNotes: ObserveNotesUseCase,
    observeFolders: ObserveFoldersUseCase,
    private val searchNotes: SearchNotesUseCase,
    private val createFolder: CreateFolderUseCase,
    private val renameFolder: RenameFolderUseCase,
    private val deleteFolder: DeleteFolderUseCase,
    private val moveNoteToFolder: MoveNoteToFolderUseCase,
    observeDeveloperOptions: ObserveDeveloperOptionsUseCase,
    observeCustomNoteStyles: ObserveCustomNoteStylesUseCase,
    isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase,
    private val getNote: GetNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val prepareNoteShare: PrepareNoteShareUseCase,
    private val clearShareCache: ClearShareCacheUseCase,
    private val shouldRequestReview: ShouldRequestReviewUseCase,
    private val markReviewAttempt: MarkReviewAttemptUseCase,
    val reviewLauncher: InAppReviewLauncher,
    private val audioPlayer: AudioPlayer,
    private val observeNoteSuggestions: ObserveNoteSuggestionsUseCase,
    private val requestNoteSuggestions: RequestNoteSuggestionsUseCase,
    private val updateSuggestionStatus: UpdateSuggestionStatusUseCase,
    private val isCalendarSuggestionsAvailable: IsCalendarSuggestionsAvailableUseCase,
    isDocumentExportAvailable: IsDocumentExportAvailableUseCase,
    private val isSmartSuggestionsEnabled: IsSmartSuggestionsEnabledUseCase,
    private val proUpgradeGate: ProUpgradeGate,
    private val sessionManager: RecordingSessionManager,
    aiCoreDownloadStatus: AiCoreDownloadStatus,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = mutableUiState.asStateFlow()

    private val mutableReviewRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reviewRequests: SharedFlow<Unit> = mutableReviewRequests.asSharedFlow()

    private var selectedNote: Note? = null
    private var noteSuggestions: NoteSuggestions? = null
    private var pendingSuggestionIndex: Int? = null
    private val selectedNoteId = MutableStateFlow<Long?>(null)
    private val allNotes = MutableStateFlow<List<Note>>(emptyList())
    private val folders = MutableStateFlow<List<Folder>>(emptyList())
    private val selectedFolderId = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            observeNotes().collect { notes ->
                allNotes.value = notes
                refreshSelected(notes)
            }
        }
        viewModelScope.launch {
            observeFolders().collect { latest ->
                folders.value = latest
                if (selectedFolderId.value != null && latest.none { it.id == selectedFolderId.value }) {
                    selectedFolderId.value = null
                }
            }
        }
        viewModelScope.launch {
            combine(allNotes, folders, selectedFolderId, searchQuery, ::listContent)
                .collect { content -> mutableUiState.update { content(it) } }
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
            combine(observeCustomNoteStyles(), isCustomNoteStylesAvailable()) { styles, unlocked ->
                styles.map { style -> style.toOptionUi() } to unlocked
            }.collect { (styles, unlocked) ->
                mutableUiState.update { it.copy(customStyles = styles, isCustomStylesUnlocked = unlocked) }
            }
        }
        viewModelScope.launch {
            sessionManager.noteProgress.collect { progress ->
                mutableUiState.update { it.copy(noteProgress = progress) }
            }
        }
        viewModelScope.launch {
            sessionManager.events.collect { event ->
                if (event is NoteProcessingEvent.ImportRejected) {
                    mutableUiState.update { it.copy(importMessage = event.reason.toMessageUi()) }
                }
            }
        }
        viewModelScope.launch {
            isDocumentExportAvailable().collect { unlocked ->
                mutableUiState.update { it.copy(isDocumentExportUnlocked = unlocked) }
            }
        }
        viewModelScope.launch {
            observeSmartSuggestions().collect { suggestions ->
                mutableUiState.update { it.copy(smartSuggestions = suggestions) }
            }
        }
        viewModelScope.launch {
            aiCoreDownloadStatus.state.collect { downloadState ->
                mutableUiState.update { it.copy(modelPreparation = downloadState.toPreparationUi()) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSmartSuggestions(): Flow<SmartSuggestionsUi?> = selectedNoteId.flatMapLatest { noteId ->
        if (noteId == null) return@flatMapLatest flowOf(null)
        combine(
            observeNoteSuggestions(noteId),
            sessionManager.suggestingNoteIds.map { noteId in it },
            isCalendarSuggestionsAvailable(),
            isSmartSuggestionsEnabled(),
            allNotes.map { notes -> notes.firstOrNull { it.id == noteId }?.status },
        ) { suggestions: NoteSuggestions?, isSuggesting: Boolean, unlocked: Boolean, enabled: Boolean, status: NoteStatus? ->
            noteSuggestions = suggestions
            suggestions.toSmartSuggestionsUi(isSuggesting, unlocked, enabled, status)
        }
    }

    // The Settings toggle hides the section for everyone; a free user who has
    // it on (a lapsed purchase) sees the locked card that leads to the paywall.
    private fun NoteSuggestions?.toSmartSuggestionsUi(
        isSuggesting: Boolean,
        unlocked: Boolean,
        enabled: Boolean,
        status: NoteStatus?,
    ): SmartSuggestionsUi? = when {
        !enabled || status != NoteStatus.READY -> null
        !unlocked -> SmartSuggestionsUi.Locked
        isSuggesting -> SmartSuggestionsUi.Loading
        this == null -> SmartSuggestionsUi.NotRun
        else -> toUi(dateLabelFormatter)
    }

    private fun listContent(
        notes: List<Note>,
        folders: List<Folder>,
        folderId: Long?,
        query: String,
    ): (NotesUiState) -> NotesUiState {
        val inFolder = if (folderId == null) notes else notes.filter { it.folderId == folderId }
        val sections = searchNotes(inFolder, query).toSectionsUi(dateLabelFormatter, folders.namesById())
        val foldersUi = folders.toFoldersUi(notes)
        return { state -> state.copy(sections = sections, folders = foldersUi, selectedFolderId = folderId) }
    }

    private fun List<Folder>.namesById(): Map<Long, String> = associate { it.id to it.name }

    fun onFolderSelected(folderId: Long?) {
        selectedFolderId.value = folderId
    }

    fun onNewFolderRequested() {
        mutableUiState.update { it.copy(folderEditor = FolderEditorUi(folderId = null, name = "")) }
    }

    fun onRenameFolderRequested(folderId: Long) {
        val folder = folders.value.firstOrNull { it.id == folderId } ?: return
        mutableUiState.update { it.copy(folderEditor = FolderEditorUi(folderId = folderId, name = folder.name)) }
    }

    fun onFolderNameChanged(name: String) {
        mutableUiState.update { state ->
            state.copy(folderEditor = state.folderEditor?.copy(name = name, error = null))
        }
    }

    fun onFolderEditorDismissed() {
        mutableUiState.update { it.copy(folderEditor = null) }
    }

    fun onFolderEditorConfirmed() {
        val editor = mutableUiState.value.folderEditor ?: return
        launchSafely(showLoading = false) {
            val result = editor.folderId?.let { renameFolder(it, editor.name) } ?: createFolder(editor.name)
            when (result) {
                is FolderSaveResult.Rejected -> mutableUiState.update { state ->
                    state.copy(folderEditor = state.folderEditor?.copy(error = result.error.toUi()))
                }
                is FolderSaveResult.Saved -> {
                    if (editor.folderId == null) selectedFolderId.value = result.folderId
                    mutableUiState.update { it.copy(folderEditor = null) }
                }
            }
        }
    }

    fun onDeleteFolderRequested(folderId: Long) {
        mutableUiState.update { it.copy(pendingDeleteFolderId = folderId) }
    }

    fun onDeleteFolderDismissed() {
        mutableUiState.update { it.copy(pendingDeleteFolderId = null) }
    }

    fun onDeleteFolderConfirmed() {
        val folderId = mutableUiState.value.pendingDeleteFolderId ?: return
        mutableUiState.update { it.copy(pendingDeleteFolderId = null) }
        launchSafely(showLoading = false) {
            deleteFolder(folderId)
        }
    }

    fun onMoveToFolderRequested() {
        val note = selectedNote ?: return
        onMoveToFolderRequested(note.id)
    }

    fun onMoveToFolderRequested(noteId: Long) {
        val note = allNotes.value.firstOrNull { it.id == noteId } ?: selectedNote?.takeIf { it.id == noteId } ?: return
        mutableUiState.update {
            it.copy(moveToFolder = MoveToFolderUi(noteId = note.id, currentFolderId = note.folderId))
        }
    }

    fun onMoveToFolderDismissed() {
        mutableUiState.update { it.copy(moveToFolder = null) }
    }

    fun onMoveToFolder(folderId: Long?) {
        val target = mutableUiState.value.moveToFolder ?: return
        mutableUiState.update { it.copy(moveToFolder = null) }
        launchSafely(showLoading = false) {
            moveNoteToFolder(target.noteId, folderId)
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        mutableUiState.update { it.copy(searchQuery = query) }
    }

    fun onNoteSelected(id: Long) {
        launchSafely(showLoading = false) {
            val note = getNote(id) ?: return@launchSafely
            selectedNote = note
            selectedNoteId.value = note.id
            loadAudio(note)
            mutableUiState.update {
                it.copy(
                    selected = note.toDetailUi(dateLabelFormatter, folders.value.namesById()),
                    editor = null,
                    pendingDeleteNoteId = null,
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
                state.copy(selected = refreshed.toDetailUi(dateLabelFormatter, folders.value.namesById()))
            }
        }
    }

    fun onDetailClosed() {
        selectedNote = null
        selectedNoteId.value = null
        pendingSuggestionIndex = null
        audioPlayer.reset()
        mutableUiState.update {
            it.copy(
                selected = null,
                editor = null,
                pendingDeleteNoteId = null,
                isShareDialogVisible = false,
                isPresetSheetVisible = false,
                moveToFolder = null,
                pendingShare = null,
                pendingCalendarEvent = null,
            )
        }
    }

    fun onPresetSheetRequested() {
        mutableUiState.update { it.copy(isPresetSheetVisible = true) }
    }

    fun onPresetSheetDismissed() {
        mutableUiState.update { it.copy(isPresetSheetVisible = false) }
    }

    fun onStyleSelected(style: NoteStyleRef) {
        val note = selectedNote ?: return
        mutableUiState.update { it.copy(isPresetSheetVisible = false) }
        launchSafely(showLoading = false) {
            if (style is NoteStyleRef.Custom && !proUpgradeGate.requirePro(ProFeature.CUSTOM_STYLES)) return@launchSafely
            recordingProcessController.restructureNote(note.id, style)
        }
    }

    fun onShareRequested() {
        mutableUiState.update { it.copy(isShareDialogVisible = true) }
    }

    fun onShareDismissed() {
        mutableUiState.update { it.copy(isShareDialogVisible = false) }
    }

    fun onShareConfirmed(noteFormat: NoteExportFormat?, includeAudio: Boolean) {
        share(noteFormat, includeAudio, saveToDevice = false)
    }

    // Save to device goes through the system file picker, which takes one file.
    fun onSaveToDeviceConfirmed(noteFormat: NoteExportFormat?, includeAudio: Boolean) {
        share(noteFormat, includeAudio, saveToDevice = true)
    }

    // PDF and Word are the Pro formats. The sheet closes before the gate is
    // asked: a modal sheet would otherwise sit above the paywall on both
    // platforms, so a declined upgrade lands back on the note.
    private fun share(noteFormat: NoteExportFormat?, includeAudio: Boolean, saveToDevice: Boolean) {
        val note = selectedNote ?: return
        if (noteFormat == null && !includeAudio) return
        mutableUiState.update { it.copy(isShareDialogVisible = false) }
        launchSafely(showLoading = false) {
            if (noteFormat.isProFormat() && !proUpgradeGate.requirePro(ProFeature.DOCUMENT_EXPORT)) return@launchSafely
            prepareShare(note, noteFormat, includeAudio, saveToDevice)
        }
    }

    private fun prepareShare(note: Note, noteFormat: NoteExportFormat?, includeAudio: Boolean, saveToDevice: Boolean) {
        launchSafely {
            val share = prepareNoteShare(note, noteFormat, includeAudio)
            mutableUiState.update { it.copy(pendingShare = share.toUi(saveToDevice)) }
        }
    }

    fun onShareLaunched() {
        mutableUiState.update { it.copy(pendingShare = null) }
    }

    fun onShareCompleted() {
        launchSafely(showLoading = false) {
            clearShareCache()
        }
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
            audioPlayer.load(fileName)
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
                it.copy(selected = updated.toDetailUi(dateLabelFormatter, folders.value.namesById()), editor = null)
            }
        }
    }

    fun onSuggestionsRequested() {
        val note = selectedNote ?: return
        launchSafely(showLoading = false) {
            if (!proUpgradeGate.requirePro(ProFeature.SMART_SUGGESTIONS)) return@launchSafely
            requestNoteSuggestions(note.id)
        }
    }

    fun onSuggestionAddRequested(index: Int) {
        val event = noteSuggestions?.events?.getOrNull(index)?.event ?: return
        pendingSuggestionIndex = index
        mutableUiState.update { it.copy(pendingCalendarEvent = event) }
    }

    fun onCalendarEventLaunched(isAdded: Boolean) {
        val index = pendingSuggestionIndex
        pendingSuggestionIndex = null
        mutableUiState.update { it.copy(pendingCalendarEvent = null) }
        if (isAdded && index != null) setSuggestionStatus(index, SuggestionStatus.ADDED)
    }

    fun onSuggestionDismissed(index: Int) {
        setSuggestionStatus(index, SuggestionStatus.DISMISSED)
    }

    private fun setSuggestionStatus(index: Int, status: SuggestionStatus) {
        val noteId = noteSuggestions?.noteId ?: return
        launchSafely(showLoading = false) {
            updateSuggestionStatus(noteId, index, status)
        }
    }

    fun onImportMessageDismissed() {
        mutableUiState.update { it.copy(importMessage = null) }
    }

    private fun ImportRejection.toMessageUi(): ImportMessageUi = when (this) {
        ImportRejection.UNSUPPORTED -> ImportMessageUi.UNSUPPORTED
        ImportRejection.TOO_LONG -> ImportMessageUi.TOO_LONG
        ImportRejection.UNREADABLE -> ImportMessageUi.UNREADABLE
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

    fun onDeleteRequested(id: Long) {
        mutableUiState.update { it.copy(pendingDeleteNoteId = id) }
    }

    fun onDeleteDismissed() {
        mutableUiState.update { it.copy(pendingDeleteNoteId = null) }
    }

    fun onDeleteConfirmed() {
        val noteId = mutableUiState.value.pendingDeleteNoteId ?: return
        mutableUiState.update { it.copy(pendingDeleteNoteId = null) }
        launchSafely {
            val isSelectedNote = selectedNote?.id == noteId
            if (isSelectedNote) {
                audioPlayer.reset()
                selectedNote = null
            }
            deleteNote(noteId)
            if (isSelectedNote) {
                mutableUiState.update { it.copy(selected = null, editor = null) }
            }
        }
    }

    override fun onCleared() {
        audioPlayer.release()
        super.onCleared()
    }
}

private fun NoteExportFormat?.isProFormat(): Boolean = this != null && this != NoteExportFormat.TEXT
