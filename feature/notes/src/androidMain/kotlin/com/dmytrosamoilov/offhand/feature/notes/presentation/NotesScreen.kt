package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.designsystem.component.CollapsibleCard
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteExportFormat
import androidx.compose.material3.FilterChip
import com.dmytrosamoilov.offhand.core.designsystem.component.LabelPill
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
import androidx.compose.runtime.Immutable
import com.dmytrosamoilov.offhand.core.designsystem.component.CollapsibleCardAction
import com.dmytrosamoilov.offhand.core.designsystem.component.MarkdownText
import com.dmytrosamoilov.offhand.core.ui.rememberSensitiveClipboard
import com.dmytrosamoilov.offhand.core.designsystem.component.MorphingLoadingIndicator
import com.dmytrosamoilov.offhand.core.designsystem.component.RoundedCheckbox
import com.dmytrosamoilov.offhand.core.designsystem.theme.extendedColors
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.NoteStylePickerSheet
import com.dmytrosamoilov.offhand.core.ui.component.CustomNoteStyleIcon
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleCard
import com.dmytrosamoilov.offhand.core.ui.component.toDomain
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.feature.notes.R
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameValidator
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import kotlinx.coroutines.delay
import com.dmytrosamoilov.offhand.core.designsystem.focus.userInitiatedFocusOnly
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotesScreen(
    onNewRecording: () -> Unit,
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val paneScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var pendingSavePath by remember { mutableStateOf<String?>(null) }
    val saveContract = remember { SaveNoteDocumentContract() }
    val saveLauncher = rememberLauncherForActivityResult(saveContract) { target ->
        val source = pendingSavePath
        pendingSavePath = null
        if (target != null && source != null) {
            paneScope.launch {
                withContext(Dispatchers.IO) { copyToDocument(context, source, target) }
                viewModel.onShareCompleted()
            }
        }
    }
    LaunchedEffect(state.pendingShare) {
        val share = state.pendingShare ?: return@LaunchedEffect
        if (share.saveToDevice) {
            pendingSavePath = share.filePaths.first()
            saveLauncher.launch(share)
        } else {
            context.startActivity(NoteShareIntentFactory.createChooser(context, share))
        }
        viewModel.onShareLaunched()
    }

    LaunchedEffect(Unit) {
        viewModel.reviewRequests.collect {
            val activity = context.findActivity() ?: return@collect
            val reviewLauncher = viewModel.reviewLauncher as AndroidInAppReviewLauncher
            if (reviewLauncher.launch(activity)) {
                viewModel.onReviewAttemptSucceeded()
            }
        }
    }

    LaunchedEffect(requestedNoteId) {
        if (requestedNoteId != null) {
            viewModel.onNoteSelected(requestedNoteId)
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, requestedNoteId)
            onRequestedNoteConsumed()
        }
    }

    var hadSelection by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.selected) {
        if (state.selected != null) {
            hadSelection = true
        } else if (hadSelection) {
            hadSelection = false
            if (navigator.canNavigateBack()) navigator.navigateBack()
        }
    }

    LaunchedEffect(state.selected?.id, state.folderEditor) {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    NotesListPane(
                        sections = state.sections,
                        searchQuery = state.searchQuery,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        folders = state.folders,
                        selectedFolderId = state.selectedFolderId,
                        onFolderSelected = viewModel::onFolderSelected,
                        onNewFolder = viewModel::onNewFolderRequested,
                        onRenameFolder = viewModel::onRenameFolderRequested,
                        onDeleteFolder = viewModel::onDeleteFolderRequested,
                        modelPreparation = state.modelPreparation,
                        onNoteClick = { id ->
                            focusManager.clearFocus()
                            viewModel.onNoteSelected(id)
                            paneScope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                            }
                        },
                        onDeleteRequested = viewModel::onDeleteRequested,
                        onMoveRequested = viewModel::onMoveToFolderRequested,
                        onNewRecording = onNewRecording,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    NoteDetailPane(
                        state = state,
                        viewModel = viewModel,
                    )
                }
            },
        )
    }

    if (state.pendingDeleteNoteId != null) {
        DeleteConfirmationDialog(
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }

    state.folderEditor?.let { editor ->
        FolderEditorDialog(
            editor = editor,
            onNameChanged = viewModel::onFolderNameChanged,
            onConfirm = viewModel::onFolderEditorConfirmed,
            onDismiss = viewModel::onFolderEditorDismissed,
        )
    }

    if (state.pendingDeleteFolderId != null) {
        DeleteFolderDialog(
            onConfirm = viewModel::onDeleteFolderConfirmed,
            onDismiss = viewModel::onDeleteFolderDismissed,
        )
    }

    state.moveToFolder?.let { target ->
        MoveToFolderSheet(
            folders = state.folders,
            currentFolderId = target.currentFolderId,
            onMove = viewModel::onMoveToFolder,
            onDismiss = viewModel::onMoveToFolderDismissed,
        )
    }

    if (state.isRetranscribeConfirmationVisible) {
        RetranscribeConfirmationDialog(
            onConfirm = viewModel::onRetranscribeConfirmed,
            onDismiss = viewModel::onRetranscribeDismissed,
        )
    }

    if (state.isShareDialogVisible) {
        ShareNoteSheet(
            hasAudio = state.selected?.hasAudio == true,
            isDocumentExportUnlocked = state.isDocumentExportUnlocked,
            onConfirm = viewModel::onShareConfirmed,
            onSaveToDevice = viewModel::onSaveToDeviceConfirmed,
            onDismiss = viewModel::onShareDismissed,
        )
    }

    state.importMessage?.let { message ->
        ImportMessageDialog(message = message, onDismiss = viewModel::onImportMessageDismissed)
    }
    CalendarEventLauncher(event = state.pendingCalendarEvent, onLaunched = viewModel::onCalendarEventLaunched)
    val selectedStyle = state.selected?.style
    if (state.isPresetSheetVisible && selectedStyle != null) {
        NoteStyleSheet(
            selected = selectedStyle,
            customStyles = state.customStyles,
            isCustomStylesUnlocked = state.isCustomStylesUnlocked,
            onSelected = viewModel::onStyleSelected,
            onDismiss = viewModel::onPresetSheetDismissed,
        )
    }
}

@Composable
private fun NoteStyleSheet(
    selected: NoteStyleRef,
    customStyles: List<NoteStyleOptionUi>,
    isCustomStylesUnlocked: Boolean,
    onSelected: (NoteStyleRef) -> Unit,
    onDismiss: () -> Unit,
) {
    NoteStylePickerSheet(
        title = stringResource(R.string.notes_preset_sheet_title),
        body = stringResource(R.string.notes_preset_sheet_body),
        selected = selected,
        customStyles = customStyles.map { NoteStyleChoice(id = it.id, name = it.name, description = it.description) },
        isCustomStylesUnlocked = isCustomStylesUnlocked,
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}

private enum class ShareTabUi { TEXT, AUDIO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareNoteSheet(
    hasAudio: Boolean,
    isDocumentExportUnlocked: Boolean,
    onConfirm: (NoteExportFormat?, Boolean) -> Unit,
    onSaveToDevice: (NoteExportFormat?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(ShareTabUi.TEXT) }
    var noteFormat by remember { mutableStateOf(NoteExportFormat.TEXT) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ShareNoteSheetContent(
            tab = tab,
            noteFormat = noteFormat,
            hasAudio = hasAudio,
            isDocumentExportUnlocked = isDocumentExportUnlocked,
            onTabSelected = { tab = it },
            onFormatSelected = { noteFormat = it },
            onShare = {
                val isText = tab == ShareTabUi.TEXT
                onConfirm(noteFormat.takeIf { isText }, !isText)
            },
            onSaveToDevice = {
                val isText = tab == ShareTabUi.TEXT
                onSaveToDevice(noteFormat.takeIf { isText }, !isText)
            },
        )
    }
}

@Composable
private fun ShareNoteSheetContent(
    tab: ShareTabUi,
    noteFormat: NoteExportFormat,
    hasAudio: Boolean,
    isDocumentExportUnlocked: Boolean,
    onTabSelected: (ShareTabUi) -> Unit,
    onFormatSelected: (NoteExportFormat) -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.notes_share_dialog_title),
            style = MaterialTheme.typography.titleLarge,
        )
        ShareTabs(selected = tab, hasAudio = hasAudio, onSelected = onTabSelected)
        when (tab) {
            ShareTabUi.TEXT -> ShareFormatOptions(
                selected = noteFormat,
                isDocumentExportUnlocked = isDocumentExportUnlocked,
                onSelected = onFormatSelected,
            )
            ShareTabUi.AUDIO -> ShareAudioDetails()
        }
        Text(
            text = stringResource(R.string.notes_share_dialog_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ShareActions(onShare = onShare, onSaveToDevice = onSaveToDevice)
    }
}

// A locked format keeps both buttons live and plain: the row already carries
// the Pro marker, and the ViewModel opens the paywall before any export.
@Composable
private fun ShareActions(onShare: () -> Unit, onSaveToDevice: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onSaveToDevice, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.notes_share_save), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(onClick = onShare, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.notes_share_dialog_confirm), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShareTabs(selected: ShareTabUi, hasAudio: Boolean, onSelected: (ShareTabUi) -> Unit) {
    val haptics = haptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ShareTabUi.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = tab == selected,
                enabled = tab == ShareTabUi.TEXT || hasAudio,
                onClick = {
                    haptics.tick()
                    onSelected(tab)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ShareTabUi.entries.size),
            ) {
                Text(text = stringResource(tab.labelRes()))
            }
        }
    }
}

@Composable
private fun ShareFormatOptions(
    selected: NoteExportFormat,
    isDocumentExportUnlocked: Boolean,
    onSelected: (NoteExportFormat) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteExportFormat.entries.forEach { format ->
            ShareFormatCard(
                format = format,
                isSelected = format == selected,
                isLocked = format.isLocked(isDocumentExportUnlocked),
                onClick = { onSelected(format) },
            )
        }
    }
}

@Composable
private fun ShareFormatCard(
    format: NoteExportFormat,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShareOptionLabel(
                label = stringResource(format.labelRes()),
                hint = stringResource(format.hintRes()),
                isDimmed = false,
                badge = { if (isLocked) ProBadge() },
                modifier = Modifier.weight(1f),
            )
            RoundedCheckbox(checked = isSelected)
        }
    }
}

@Composable
private fun ShareAudioDetails() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ShareOptionLabel(
                label = stringResource(R.string.notes_share_audio_format),
                hint = stringResource(R.string.notes_share_audio_hint),
                isDimmed = false,
                badge = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.notes_share_audio_details),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShareOptionLabel(
    label: String,
    hint: String,
    isDimmed: Boolean,
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            badge()
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun NoteExportFormat.isLocked(isDocumentExportUnlocked: Boolean): Boolean =
    this != NoteExportFormat.TEXT && !isDocumentExportUnlocked

private fun ShareTabUi.labelRes(): Int = when (this) {
    ShareTabUi.TEXT -> R.string.notes_share_tab_text
    ShareTabUi.AUDIO -> R.string.notes_share_tab_audio
}

private fun NoteExportFormat.labelRes(): Int = when (this) {
    NoteExportFormat.TEXT -> R.string.notes_share_format_text
    NoteExportFormat.PDF -> R.string.notes_share_format_pdf
    NoteExportFormat.DOCX -> R.string.notes_share_format_docx
}

private fun NoteExportFormat.hintRes(): Int = when (this) {
    NoteExportFormat.TEXT -> R.string.notes_share_format_text_hint
    NoteExportFormat.PDF -> R.string.notes_share_format_pdf_hint
    NoteExportFormat.DOCX -> R.string.notes_share_format_docx_hint
}

@Composable
private fun RetranscribeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.notes_retranscribe_dialog_title)) },
        text = { Text(text = stringResource(R.string.notes_retranscribe_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.notes_retranscribe_description))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.notes_delete_dialog_cancel))
            }
        },
    )
}

@Composable
private fun NotesListPane(
    sections: List<NotesSectionUi>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    folders: List<FolderUi>,
    selectedFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onNewFolder: () -> Unit,
    onRenameFolder: (Long) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    modelPreparation: ModelPreparationUi?,
    onNoteClick: (Long) -> Unit,
    onDeleteRequested: (Long) -> Unit,
    onMoveRequested: (Long) -> Unit,
    onNewRecording: () -> Unit,
) {
    val listState = rememberLazyListState()
    // Foundation ≥1.8 anchors prepended items above the viewport; re-pin to the
    // top when a new note arrives unless the user has scrolled away.
    LaunchedEffect(sections.firstOrNull()?.notes?.firstOrNull()?.id) {
        if (listState.firstVisibleItemIndex <= 1) {
            listState.scrollToItem(0)
        }
    }
    val haptics = haptics()
    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.notes_title))
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.confirm()
                    onNewRecording()
                },
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.notes_record_fab_description),
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .userInitiatedFocusOnly(),
        ) {
            ModelPreparationBanner(preparation = modelPreparation)
            val isLibraryEmpty = sections.isEmpty() && folders.isEmpty() &&
                searchQuery.isBlank() && selectedFolderId == null
            if (isLibraryEmpty) {
                EmptyListMessage(text = stringResource(R.string.notes_empty_state))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = SEARCH_ITEM_KEY, contentType = "search") {
                        NotesSearchField(query = searchQuery, onQueryChanged = onSearchQueryChanged)
                    }
                    item(key = FOLDERS_ITEM_KEY, contentType = "folders") {
                        FolderChips(
                            folders = folders,
                            selectedFolderId = selectedFolderId,
                            onFolderSelected = onFolderSelected,
                            onNewFolder = onNewFolder,
                            onRenameFolder = onRenameFolder,
                            onDeleteFolder = onDeleteFolder,
                        )
                    }
                    if (sections.isEmpty()) {
                        item(key = EMPTY_SEARCH_ITEM_KEY, contentType = "empty") {
                            EmptyListMessage(text = stringResource(emptyMessageRes(searchQuery, selectedFolderId)))
                        }
                    }
                    sections.forEach { section ->
                        item(key = section.dayLabel.headerKey(), contentType = "header") {
                            SectionHeader(dayLabel = section.dayLabel)
                        }
                        items(section.notes, key = { it.id }) { note ->
                            SwipeableNoteCard(
                                onDeleteRequested = { onDeleteRequested(note.id) },
                                onMoveRequested = { onMoveRequested(note.id) },
                                modifier = Modifier.animateItem(),
                            ) {
                                NoteCard(note = note, onClick = { onNoteClick(note.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val SEARCH_ITEM_KEY = "search"
private const val FOLDERS_ITEM_KEY = "folders"
private const val EMPTY_SEARCH_ITEM_KEY = "empty-search"

private fun emptyMessageRes(searchQuery: String, selectedFolderId: Long?): Int = when {
    searchQuery.isNotBlank() -> R.string.notes_search_empty_state
    selectedFolderId != null -> R.string.notes_folder_empty_state
    else -> R.string.notes_empty_state
}

@Composable
private fun FolderChips(
    folders: List<FolderUi>,
    selectedFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onNewFolder: () -> Unit,
    onRenameFolder: (Long) -> Unit,
    onDeleteFolder: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item(key = ALL_NOTES_CHIP_KEY) {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onFolderSelected(null) },
                label = { Text(text = stringResource(R.string.notes_folder_all)) },
            )
        }
        items(folders, key = { it.id }) { folder ->
            FolderChip(
                folder = folder,
                isSelected = folder.id == selectedFolderId,
                onSelected = { onFolderSelected(folder.id) },
                onRename = { onRenameFolder(folder.id) },
                onDelete = { onDeleteFolder(folder.id) },
            )
        }
        item(key = NEW_FOLDER_CHIP_KEY) { NewFolderChip(onClick = onNewFolder) }
    }
}

private const val ALL_NOTES_CHIP_KEY = "all-notes"
private const val NEW_FOLDER_CHIP_KEY = "new-folder"

@Composable
private fun NewFolderChip(onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = stringResource(R.string.notes_folder_new)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        },
    )
}

@Composable
private fun FolderChip(
    folder: FolderUi,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = isSelected,
            onClick = { if (isSelected) isMenuExpanded = true else onSelected() },
            label = { Text(text = folder.name) },
            trailingIcon = if (isSelected) {
                { FolderOptionsIcon() }
            } else {
                null
            },
        )
        FolderChipMenu(
            expanded = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            onRename = onRename,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun FolderOptionsIcon() {
    Icon(
        imageVector = Icons.Filled.MoreVert,
        contentDescription = stringResource(R.string.notes_folder_options_description),
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun FolderChipMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.notes_folder_rename)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onRename()
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.notes_folder_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}

@Composable
private fun FolderEditorDialog(
    editor: FolderEditorUi,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (editor.folderId == null) R.string.notes_folder_new else R.string.notes_folder_rename,
                ),
            )
        },
        text = { FolderNameField(editor = editor, onNameChanged = onNameChanged, onDone = onConfirm) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.notes_folder_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.notes_delete_dialog_cancel))
            }
        },
    )
}

@Composable
private fun FolderNameField(
    editor: FolderEditorUi,
    onNameChanged: (String) -> Unit,
    onDone: () -> Unit,
) {
    var value by remember(editor.folderId) {
        mutableStateOf(TextFieldValue(editor.name, TextRange(0, editor.name.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = value,
        onValueChange = { changed ->
            value = changed
            onNameChanged(changed.text)
        },
        modifier = Modifier.focusRequester(focusRequester),
        singleLine = true,
        label = { Text(text = stringResource(R.string.notes_folder_name_label)) },
        isError = editor.error != null,
        supportingText = editor.error?.let { error -> { Text(text = error.message()) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun FolderNameErrorUi.message(): String = when (this) {
    FolderNameErrorUi.BLANK -> stringResource(R.string.notes_folder_error_blank)
    FolderNameErrorUi.TOO_LONG -> stringResource(R.string.notes_folder_error_too_long, FolderNameValidator.MAX_LENGTH)
    FolderNameErrorUi.DUPLICATE -> stringResource(R.string.notes_folder_error_duplicate)
}

@Composable
private fun DeleteFolderDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.notes_folder_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.notes_folder_delete_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.notes_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.notes_delete_dialog_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderSheet(
    folders: List<FolderUi>,
    currentFolderId: Long?,
    onMove: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.notes_move_to_folder),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            FolderChoiceRow(
                label = stringResource(R.string.notes_folder_none),
                isSelected = currentFolderId == null,
                onClick = { onMove(null) },
            )
            folders.forEach { folder ->
                FolderChoiceRow(
                    label = folder.name,
                    isSelected = folder.id == currentFolderId,
                    onClick = { onMove(folder.id) },
                )
            }
        }
    }
}

@Composable
private fun FolderChoiceRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyListMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun NotesSearchField(query: String, onQueryChanged: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { Text(text = stringResource(R.string.notes_search_placeholder)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = { if (query.isNotEmpty()) ClearSearchButton(onClick = { onQueryChanged("") }) },
            singleLine = true,
            shape = CircleShape,
            colors = searchFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        )
        AnimatedVisibility(visible = isFocused, enter = fadeIn(), exit = fadeOut()) {
            CancelSearchButton(
                onClick = {
                    onQueryChanged("")
                    focusManager.clearFocus()
                },
            )
        }
    }
}

@Composable
private fun searchFieldColors(): TextFieldColors {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    return TextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )
}

@Composable
private fun CancelSearchButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.padding(start = 4.dp)) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.notes_search_cancel_description),
        )
    }
}

@Composable
private fun ClearSearchButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = stringResource(R.string.notes_search_clear_description),
        )
    }
}

@Composable
private fun ModelPreparationBanner(preparation: ModelPreparationUi?) {
    var lastVisible by remember { mutableStateOf(ModelPreparationUi(progressPercent = 0)) }
    if (preparation != null) {
        lastVisible = preparation
    }
    AnimatedVisibility(
        visible = preparation != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val progress by animateFloatAsState(
            targetValue = lastVisible.progressPercent / 100f,
            label = "modelPreparationProgress",
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MorphingLoadingIndicator(modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notes_model_banner_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(R.string.notes_model_banner_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            R.string.notes_model_banner_percent,
                            lastVisible.progressPercent,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun NoteDayLabelUi.headerKey(): String = when (this) {
    NoteDayLabelUi.Today -> "header-today"
    NoteDayLabelUi.Yesterday -> "header-yesterday"
    is NoteDayLabelUi.Date -> "header-$text"
}

@Composable
private fun NoteDayLabelUi.text(): String = when (this) {
    NoteDayLabelUi.Today -> stringResource(R.string.notes_day_today)
    NoteDayLabelUi.Yesterday -> stringResource(R.string.notes_day_yesterday)
    is NoteDayLabelUi.Date -> text
}

@Composable
private fun SectionHeader(dayLabel: NoteDayLabelUi) {
    Text(
        text = dayLabel.text(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun SwipeableNoteCard(
    onDeleteRequested: () -> Unit,
    onMoveRequested: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnDeleteRequested by rememberUpdatedState(onDeleteRequested)
    val currentOnMoveRequested by rememberUpdatedState(onMoveRequested)
    val haptics = haptics()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.gestureEnd()
                    currentOnDeleteRequested()
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.gestureEnd()
                    currentOnMoveRequested()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            value == SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(direction = dismissState.dismissDirection) },
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val isMove = direction == SwipeToDismissBoxValue.StartToEnd
    val containerColor = if (isMove) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (isMove) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = containerColor, shape = CardDefaults.shape)
            .padding(horizontal = 24.dp),
        contentAlignment = if (isMove) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = if (isMove) Icons.Filled.DriveFileMove else Icons.Filled.Delete,
            contentDescription = null,
            tint = contentColor,
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteCardUi,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NoteCardIcon(status = note.status)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = note.cardTitle().highlighted(note.titleHighlights),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.notes_created_at,
                            note.dayLabel.text(),
                            note.time,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.cardPreview(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.status == NoteStatusUi.READY && (note.durationText != null || note.folderName != null)) {
                Spacer(modifier = Modifier.height(12.dp))
                NoteCardMetadata(durationText = note.durationText, folderName = note.folderName)
            }
        }
    }
}

@Composable
private fun NoteCardMetadata(durationText: String?, folderName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (durationText != null) MetadataWithIcon(icon = Icons.Filled.Mic, text = durationText)
        if (folderName != null) MetadataWithIcon(icon = Icons.Filled.Folder, text = folderName)
    }
}

@Composable
private fun MetadataWithIcon(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MetadataText(text = text)
    }
}

@Composable
private fun MetadataText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NoteCardIcon(status: NoteStatusUi) {
    val containerColor = when (status) {
        NoteStatusUi.FAILED -> MaterialTheme.extendedColors.warningContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            NoteStatusUi.PROCESSING -> MorphingLoadingIndicator(
                modifier = Modifier.size(26.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            NoteStatusUi.READY -> Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            NoteStatusUi.FAILED -> Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.extendedColors.onWarningContainer,
            )
        }
    }
}

@Composable
private fun NoteCardUi.cardTitle(): String = when (status) {
    NoteStatusUi.PROCESSING -> title.ifBlank { stringResource(R.string.notes_processing_title) }
    NoteStatusUi.FAILED -> title.ifBlank { stringResource(R.string.notes_recording_fallback_title) }
    NoteStatusUi.READY -> title
}

@Composable
private fun NoteCardUi.cardPreview(): AnnotatedString = when (status) {
    NoteStatusUi.PROCESSING -> AnnotatedString(stringResource(R.string.notes_processing_preview))
    NoteStatusUi.FAILED -> AnnotatedString(stringResource(R.string.notes_failed_description))
    NoteStatusUi.READY -> preview.highlighted(previewHighlights)
}

@Composable
private fun String.highlighted(ranges: List<TextRangeUi>): AnnotatedString {
    val style = SpanStyle(background = MaterialTheme.extendedColors.searchHighlight)
    return buildAnnotatedString {
        append(this@highlighted)
        ranges.forEach { range -> addStyle(style, range.start, range.end.coerceAtMost(length)) }
    }
}

@Composable
private fun NoteDetailPane(
    state: NotesUiState,
    viewModel: NotesViewModel,
) {
    val selected = state.selected
    val editor = state.editor
    when {
        selected == null -> EmptyDetailPlaceholder()
        editor != null -> NoteEditor(
            editor = editor,
            onTitleChanged = viewModel::onEditorTitleChanged,
            onTranscriptChanged = viewModel::onEditorTranscriptChanged,
            onSave = viewModel::onEditSaved,
            onCancel = viewModel::onEditCancelled,
        )
        else -> NoteDetail(
            note = selected,
            playback = state.playback,
            smartSuggestions = state.smartSuggestions,
            showMetrics = state.isDeveloperMode,
            progressPercent = state.noteProgress[selected.id],
            onBack = viewModel::onDetailClosed,
            onEdit = viewModel::onEditStarted,
            onShareRequested = viewModel::onShareRequested,
            onDeleteRequested = { viewModel.onDeleteRequested(selected.id) },
            onPlayPause = viewModel::onPlayPauseClicked,
            onSeek = viewModel::onSeekRequested,
            onRetryTranscription = viewModel::onRetryTranscriptionRequested,
            onRetranscribeRequested = viewModel::onRetranscribeRequested,
            onPresetRequested = viewModel::onPresetSheetRequested,
            onMoveToFolderRequested = viewModel::onMoveToFolderRequested,
            suggestionActions = SuggestionActions(
                onFind = viewModel::onSuggestionsRequested,
                onAdd = viewModel::onSuggestionAddRequested,
                onDismiss = viewModel::onSuggestionDismissed,
            ),
        )
    }
}

@Composable
private fun EmptyDetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.notes_select_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteDetail(
    note: NoteDetailUi,
    playback: AudioPlaybackUi,
    smartSuggestions: SmartSuggestionsUi?,
    showMetrics: Boolean,
    progressPercent: Int?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShareRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRetryTranscription: () -> Unit,
    onRetranscribeRequested: () -> Unit,
    onPresetRequested: () -> Unit,
    onMoveToFolderRequested: () -> Unit,
    suggestionActions: SuggestionActions,
) {
    Scaffold(
        topBar = {
            NoteDetailTopBar(
                showEditActions = note.status == NoteStatusUi.READY,
                note = note,
                onBack = onBack,
                onEdit = onEdit,
                onShareRequested = onShareRequested,
                onDeleteRequested = onDeleteRequested,
                onRetranscribeRequested = onRetranscribeRequested,
                onPresetRequested = onPresetRequested,
                onMoveToFolderRequested = onMoveToFolderRequested,
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        NoteDetailContent(
            note = note,
            playback = playback,
            smartSuggestions = smartSuggestions,
            suggestionActions = suggestionActions,
            showMetrics = showMetrics,
            progressPercent = progressPercent,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onRetryTranscription = onRetryTranscription,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun NoteDetailTopBar(
    showEditActions: Boolean,
    note: NoteDetailUi,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShareRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    onRetranscribeRequested: () -> Unit,
    onPresetRequested: () -> Unit,
    onMoveToFolderRequested: () -> Unit,
) {
    AppTopBar(
        title = "",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.notes_back_description),
                )
            }
        },
        actions = {
            if (showEditActions) {
                ShareNoteButton(onClick = onShareRequested)
                NoteOverflowMenu(
                    showRetranscribe = note.hasAudio,
                    showPreset = note.transcript.isNotBlank(),
                    onEdit = onEdit,
                    onRetranscribeRequested = onRetranscribeRequested,
                    onPresetRequested = onPresetRequested,
                    onMoveToFolderRequested = onMoveToFolderRequested,
                    onDeleteRequested = onDeleteRequested,
                )
            } else {
                IconButton(onClick = onDeleteRequested) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.notes_delete_description),
                    )
                }
            }
        },
    )
}

@Composable
private fun ShareNoteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = stringResource(R.string.notes_share_description),
        )
    }
}

@Composable
private fun NoteOverflowMenu(
    showRetranscribe: Boolean,
    showPreset: Boolean,
    onEdit: () -> Unit,
    onRetranscribeRequested: () -> Unit,
    onPresetRequested: () -> Unit,
    onMoveToFolderRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.notes_more_actions_description),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EditMenuItem(
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            MoveToFolderMenuItem(
                onClick = {
                    expanded = false
                    onMoveToFolderRequested()
                },
            )
            if (showPreset) {
                PresetMenuItem(
                    onClick = {
                        expanded = false
                        onPresetRequested()
                    },
                )
            }
            if (showRetranscribe) {
                RetranscribeMenuItem(
                    onClick = {
                        expanded = false
                        onRetranscribeRequested()
                    },
                )
            }
            DeleteMenuItem(
                onClick = {
                    expanded = false
                    onDeleteRequested()
                },
            )
        }
    }
}

@Composable
private fun EditMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.notes_edit_description)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun MoveToFolderMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.notes_move_to_folder)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.DriveFileMove, contentDescription = null)
        },
        onClick = onClick,
    )
}

@Composable
private fun PresetMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.notes_preset_description)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Tune, contentDescription = null)
        },
        onClick = onClick,
    )
}

@Composable
private fun RetranscribeMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.notes_retranscribe_description)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
        },
        onClick = onClick,
    )
}

@Composable
private fun DeleteMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(R.string.notes_delete_description),
                color = MaterialTheme.colorScheme.error,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun NoteDetailContent(
    note: NoteDetailUi,
    playback: AudioPlaybackUi,
    smartSuggestions: SmartSuggestionsUi?,
    suggestionActions: SuggestionActions,
    showMetrics: Boolean,
    progressPercent: Int?,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRetryTranscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (note.hasAudio && playback.isAvailable) {
            AudioPlayerCard(
                playback = playback,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(text = note.detailTitle(), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetadataText(text = note.createdAt)
            if (note.wordCount > 0) {
                MetadataText(text = stringResource(R.string.notes_metadata_separator))
                MetadataText(
                    text = pluralStringResource(
                        R.plurals.notes_word_count,
                        note.wordCount,
                        String.format(Locale.getDefault(), "%,d", note.wordCount),
                    ),
                )
            }
            if (note.folderName != null) {
                MetadataText(text = stringResource(R.string.notes_metadata_separator))
                MetadataText(text = note.folderName)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        TrustBadges()
        if (showMetrics && note.metrics != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(
                        R.string.notes_metrics,
                        note.metrics.transcriptionTime,
                        note.metrics.structuringTime,
                        note.metrics.hardwareBackend,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ProcessingHaptics(noteId = note.id, status = note.status)
        when (note.status) {
            NoteStatusUi.PROCESSING -> DetailStatusCard(
                text = stringResource(
                    if (note.transcript.isBlank()) {
                        R.string.notes_processing_transcribing
                    } else {
                        R.string.notes_processing_structuring
                    },
                ),
                percent = progressPercent,
            )
            NoteStatusUi.FAILED -> FailedDetailCard(
                hasTranscript = note.transcript.isNotBlank(),
                hasAudio = note.hasAudio,
                onRetryTranscription = onRetryTranscription,
            )
            NoteStatusUi.READY -> key(note.id) {
                CollapsibleCard(
                    title = stringResource(R.string.notes_overview_heading),
                    labelContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    initiallyExpanded = true,
                    action = rememberCopyAction(
                        text = note.body,
                        clipboardLabel = stringResource(R.string.notes_overview_heading),
                        contentDescription = stringResource(R.string.notes_copy_overview_description),
                    ),
                ) {
                    MarkdownText(markdown = note.body)
                }
            }
        }
        if (smartSuggestions != null) {
            Spacer(modifier = Modifier.height(16.dp))
            SmartSuggestionsCard(suggestions = smartSuggestions, actions = suggestionActions)
        }
        if (note.transcript.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            key(note.id) {
                CollapsibleCard(
                    title = stringResource(R.string.notes_transcript_heading),
                    labelContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    initiallyExpanded = note.status != NoteStatusUi.READY,
                    action = rememberCopyAction(
                        text = note.transcript,
                        clipboardLabel = stringResource(R.string.notes_transcript_heading),
                        contentDescription = stringResource(R.string.notes_copy_transcript_description),
                    ),
                ) {
                    MarkdownText(markdown = note.transcript)
                }
            }
        }
    }
}

@Composable
private fun rememberCopyAction(
    text: String,
    clipboardLabel: String,
    contentDescription: String,
): CollapsibleCardAction {
    val clipboard = rememberSensitiveClipboard()
    val haptics = haptics()
    val copiedMessage = stringResource(R.string.notes_copied)
    return remember(text, clipboardLabel, contentDescription, copiedMessage) {
        CollapsibleCardAction(
            icon = Icons.Filled.ContentCopy,
            contentDescription = contentDescription,
            onClick = {
                haptics.confirm()
                clipboard.copy(clipboardLabel, text, copiedMessage)
            },
        )
    }
}

@Composable
private fun ProcessingHaptics(noteId: Long, status: NoteStatusUi) {
    val haptics = haptics()
    var previousStatus by remember(noteId) { mutableStateOf(status) }
    LaunchedEffect(noteId, status) {
        if (previousStatus == NoteStatusUi.PROCESSING && status == NoteStatusUi.READY) haptics.confirm()
        previousStatus = status
        while (status == NoteStatusUi.PROCESSING) {
            delay(PROCESSING_TICK_MS)
            haptics.tick()
        }
    }
}

private const val PROCESSING_TICK_MS = 2_500L

@Composable
private fun TrustBadges() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrustBadge(
            icon = Icons.Filled.Lock,
            label = stringResource(R.string.notes_badge_encrypted),
        )
    }
}

@Composable
private fun TrustBadge(icon: ImageVector, label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NoteDetailUi.detailTitle(): String = when (status) {
    NoteStatusUi.PROCESSING -> title.ifBlank { stringResource(R.string.notes_processing_title) }
    NoteStatusUi.FAILED -> title.ifBlank { stringResource(R.string.notes_recording_fallback_title) }
    NoteStatusUi.READY -> title
}

@Composable
private fun FailedDetailCard(
    hasTranscript: Boolean,
    hasAudio: Boolean,
    onRetryTranscription: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    if (hasTranscript) {
                        R.string.notes_failed_overview_description
                    } else {
                        R.string.notes_failed_description
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasAudio) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetryTranscription) {
                    Text(text = stringResource(R.string.notes_try_again_button))
                }
            }
        }
    }
}

@Composable
private fun DetailStatusCard(text: String, percent: Int?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MorphingLoadingIndicator()
            if (percent != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.notes_processing_percent, percent),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AudioPlayerCard(
    playback: AudioPlaybackUi,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val haptics = haptics()
    Card(modifier = Modifier.fillMaxWidth(), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = {
                    haptics.confirm()
                    onPlayPause()
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = if (playback.isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = stringResource(
                        if (playback.isPlaying) {
                            R.string.notes_pause_description
                        } else {
                            R.string.notes_play_description
                        },
                    ),
                )
            }
            Slider(
                value = dragFraction ?: playback.progress,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let(onSeek)
                    dragFraction = null
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            Text(
                text = stringResource(
                    R.string.notes_playback_position,
                    playback.positionText,
                    playback.durationText,
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun NoteEditor(
    editor: NoteEditorUi,
    onTitleChanged: (String) -> Unit,
    onTranscriptChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.notes_edit_description),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(
                                R.string.notes_edit_cancel_description,
                            ),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(
                                R.string.notes_edit_save_description,
                            ),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.notes_edit_title_label)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = editor.transcript,
                onValueChange = onTranscriptChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.notes_transcript_heading)) },
                minLines = 10,
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.notes_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.notes_delete_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.notes_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.notes_delete_dialog_cancel))
            }
        },
    )
}

@Composable
private fun ImportMessageDialog(message: ImportMessageUi, onDismiss: () -> Unit) {
    val body = when (message) {
        ImportMessageUi.UNSUPPORTED -> R.string.notes_import_error_unsupported
        ImportMessageUi.TOO_LONG -> R.string.notes_import_error_too_long
        ImportMessageUi.UNREADABLE -> R.string.notes_import_error_unreadable
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.notes_import_error_title)) },
        text = { Text(text = stringResource(body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.notes_import_error_dismiss)) }
        },
    )
}

private const val CALENDAR_EVENT_MIME_TYPE = "vnd.android.cursor.item/event"

@Composable
private fun CalendarEventLauncher(event: CalendarEventSuggestion?, onLaunched: (Boolean) -> Unit) {
    val context = LocalContext.current
    var isCalendarMissing by remember { mutableStateOf(false) }
    LaunchedEffect(event) {
        if (event == null) return@LaunchedEffect
        val opened = openCalendarInsert(context, event)
        isCalendarMissing = !opened
        onLaunched(opened)
    }
    if (isCalendarMissing) {
        AlertDialog(
            onDismissRequest = { isCalendarMissing = false },
            title = { Text(text = stringResource(R.string.notes_calendar_sheet_title)) },
            text = { Text(text = stringResource(R.string.notes_calendar_no_app)) },
            confirmButton = {
                TextButton(onClick = { isCalendarMissing = false }) {
                    Text(text = stringResource(R.string.notes_import_error_dismiss))
                }
            },
        )
    }
}

private fun openCalendarInsert(context: Context, event: CalendarEventSuggestion): Boolean =
    startCalendarInsert(context, calendarInsertIntent(event).setData(CalendarContract.Events.CONTENT_URI)) ||
        startCalendarInsert(context, calendarInsertIntent(event).setType(CALENDAR_EVENT_MIME_TYPE))

private fun startCalendarInsert(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (missing: ActivityNotFoundException) {
    false
}

private fun calendarInsertIntent(event: CalendarEventSuggestion): Intent =
    Intent(Intent.ACTION_INSERT)
        .putExtra(CalendarContract.Events.TITLE, event.title)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startEpochMs)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endEpochMs)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.isAllDay)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        .putExtra(CalendarContract.Events.DESCRIPTION, event.details)

@Immutable
private data class SuggestionActions(
    val onFind: () -> Unit,
    val onAdd: (Int) -> Unit,
    val onDismiss: (Int) -> Unit,
)

@Composable
private fun SmartSuggestionsCard(suggestions: SmartSuggestionsUi, actions: SuggestionActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabelPill(
                    title = stringResource(R.string.notes_suggestions_heading),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (suggestions == SmartSuggestionsUi.Locked) ProBadge()
            }
            Spacer(modifier = Modifier.height(12.dp))
            SmartSuggestionsBody(suggestions = suggestions, actions = actions)
        }
    }
}

@Composable
private fun SmartSuggestionsBody(suggestions: SmartSuggestionsUi, actions: SuggestionActions) {
    when (suggestions) {
        SmartSuggestionsUi.Loading -> SuggestionsStatus(text = stringResource(R.string.notes_suggestions_loading), showProgress = true)
        SmartSuggestionsUi.Empty -> SuggestionsStatus(text = stringResource(R.string.notes_suggestions_empty))
        SmartSuggestionsUi.NotRun -> FindSuggestionsButton(onClick = actions.onFind)
        SmartSuggestionsUi.Locked -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SuggestionsStatus(text = stringResource(R.string.notes_suggestions_locked))
            FindSuggestionsButton(onClick = actions.onFind)
        }
        is SmartSuggestionsUi.Ready -> Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Max)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            suggestions.events.forEach { event ->
                key(event.index) {
                    SuggestionCard(
                        event = event,
                        onAdd = { actions.onAdd(event.index) },
                        onDismiss = { actions.onDismiss(event.index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsStatus(text: String, showProgress: Boolean = false) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showProgress) CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FindSuggestionsButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.padding(horizontal = 20.dp)) {
        Icon(imageVector = Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.notes_suggestions_find))
    }
}

@Composable
private fun SuggestionCard(event: CalendarEventUi, onAdd: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(SUGGESTION_CARD_WIDTH)
            .fillMaxHeight(),
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (event.isAllDay) "${event.whenText} · ${stringResource(R.string.notes_calendar_all_day)}" else event.whenText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SuggestionDetail(text = event.location)
            SuggestionDetail(text = event.details)
            Spacer(modifier = Modifier.weight(1f))
            SuggestionCardActions(event = event, onAdd = onAdd, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun SuggestionDetail(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SuggestionCardActions(event: CalendarEventUi, onAdd: () -> Unit, onDismiss: () -> Unit) {
    val haptics = haptics()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (event.isAdded) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(text = stringResource(R.string.notes_suggestions_added), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            FilledTonalButton(
                onClick = {
                    haptics.confirm()
                    onAdd()
                },
            ) {
                Text(text = stringResource(R.string.notes_calendar_add))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.notes_suggestions_dismiss))
        }
    }
}

private val SUGGESTION_CARD_WIDTH = 260.dp
