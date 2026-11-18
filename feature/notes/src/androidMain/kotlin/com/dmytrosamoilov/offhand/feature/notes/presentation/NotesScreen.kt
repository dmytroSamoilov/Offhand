package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotesScreen(
    onBottomBarStateChange: (NotesBottomBarState) -> Unit,
    onCreateNoteStyle: () -> Unit,
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>(scaffoldDirective = notesPaneDirective())
    val paneScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val onNewStyle = rememberStyleEditorDetour(viewModel = viewModel, onCreateNoteStyle = onCreateNoteStyle)
    NoteShareLauncher(pendingShare = state.pendingShare, viewModel = viewModel)
    ReviewRequestLauncher(viewModel = viewModel)
    DetailNavigationEffects(
        state = state,
        navigator = navigator,
        requestedNoteId = requestedNoteId,
        onRequestedNoteConsumed = onRequestedNoteConsumed,
        viewModel = viewModel,
    )
    val isTwoPane = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
    val isListShown = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] != PaneAdaptedValue.Hidden
    var isListBarWanted by rememberSaveable { mutableStateOf(true) }
    val bottomBarState = NotesBottomBarState(isVisible = isListShown && isListBarWanted, folderId = state.selectedFolderId)
    LaunchedEffect(bottomBarState) { onBottomBarStateChange(bottomBarState) }
    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    NotesListPane(
                        state = state,
                        viewModel = viewModel,
                        openNoteId = state.selected?.id?.takeIf { isTwoPane },
                        onNoteClick = { id, isSearchResult ->
                            focusManager.clearFocus()
                            if (isSearchResult) viewModel.onSearchResultSelected(id) else viewModel.onNoteSelected(id)
                            paneScope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) }
                        },
                        onBottomBarWantedChange = { isListBarWanted = it },
                    )
                }
            },
            detailPane = {
                AnimatedPane { NoteDetailPane(state = state, viewModel = viewModel, showsBack = !isTwoPane) }
            },
        )
    }
    NotesOverlays(state = state, viewModel = viewModel, onNewStyle = onNewStyle)
}

// A phone held sideways is wide enough for the list beside the note but far
// too short for it: the bottom bar and the note's own bars leave the text a
// strip. Below a medium height the note takes the whole window, as it does
// on a phone held upright.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun notesPaneDirective(): PaneScaffoldDirective {
    val windowInfo = currentWindowAdaptiveInfo()
    val directive = calculatePaneScaffoldDirective(windowInfo)
    val isShort = !windowInfo.windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    return if (isShort) directive.copy(maxHorizontalPartitions = 1) else directive
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun DetailNavigationEffects(
    state: NotesUiState,
    navigator: ThreePaneScaffoldNavigator<Long>,
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    viewModel: NotesViewModel,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
}

@Composable
private fun NoteShareLauncher(pendingShare: NoteShareUi?, viewModel: NotesViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSavePath by remember { mutableStateOf<String?>(null) }
    val saveContract = remember { SaveNoteDocumentContract() }
    val saveLauncher = rememberLauncherForActivityResult(saveContract) { target ->
        val source = pendingSavePath
        pendingSavePath = null
        if (target != null && source != null) {
            scope.launch {
                withContext(Dispatchers.IO) { copyToDocument(context, source, target) }
                viewModel.onShareCompleted()
            }
        }
    }
    LaunchedEffect(pendingShare) {
        val share = pendingShare ?: return@LaunchedEffect
        if (share.saveToDevice) {
            pendingSavePath = share.filePaths.first()
            saveLauncher.launch(share)
        } else {
            context.startActivity(NoteShareIntentFactory.createChooser(context, share))
        }
        viewModel.onShareLaunched()
    }
}

@Composable
private fun ReviewRequestLauncher(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.reviewRequests.collect {
            val activity = context.findActivity() ?: return@collect
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@collect
            val reviewLauncher = viewModel.reviewLauncher as AndroidInAppReviewLauncher
            if (reviewLauncher.launch(activity)) viewModel.onReviewAttemptSucceeded()
        }
    }
}

internal sealed interface StyleEditorOrigin {
    data object Note : StyleEditorOrigin
    data class Folder(val folderId: Long) : StyleEditorOrigin
}

@Composable
private fun rememberStyleEditorDetour(
    viewModel: NotesViewModel,
    onCreateNoteStyle: () -> Unit,
): (StyleEditorOrigin) -> Unit {
    var pending by remember { mutableStateOf<StyleEditorOrigin?>(null) }
    var reopensStyleSheet by rememberSaveable { mutableStateOf(false) }
    var reopensFolderPickerId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        if (reopensStyleSheet) viewModel.onPresetSheetRequested()
        reopensFolderPickerId?.let(viewModel::onFolderStyleRequested)
        reopensStyleSheet = false
        reopensFolderPickerId = null
    }
    LaunchedEffect(pending) {
        when (val origin = pending ?: return@LaunchedEffect) {
            StyleEditorOrigin.Note -> reopensStyleSheet = true
            is StyleEditorOrigin.Folder -> reopensFolderPickerId = origin.folderId
        }
        pending = null
        onCreateNoteStyle()
    }
    return remember { { origin -> pending = origin } }
}
