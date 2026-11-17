package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.EmptyState
import com.dmytrosamoilov.offhand.core.designsystem.component.Hairline
import com.dmytrosamoilov.offhand.core.designsystem.component.LargeTitleHeader
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel
import com.dmytrosamoilov.offhand.core.designsystem.component.Skeleton
import com.dmytrosamoilov.offhand.core.designsystem.focus.userInitiatedFocusOnly
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun NotesListPane(
    state: NotesUiState,
    viewModel: NotesViewModel,
    openNoteId: Long?,
    onNoteClick: (id: Long, isSearchResult: Boolean) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val selection = rememberNoteSelection(state = state, viewModel = viewModel)
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    val isSearching = isSearchOpen || state.searchQuery.isNotEmpty()
    val closeSearch = {
        isSearchOpen = false
        viewModel.onSearchQueryChanged("")
    }
    BackHandler(enabled = selection.isActive, onBack = selection.onClear)
    BackHandler(enabled = isSearching && !selection.isActive, onBack = closeSearch)
    Column(modifier = Modifier.fillMaxSize().background(OffhandTheme.colors.ground).userInitiatedFocusOnly()) {
        when {
            selection.isActive -> SelectionHeader(selection = selection)
            isSearching -> SearchHeader(query = state.searchQuery, onQueryChanged = viewModel::onSearchQueryChanged, onCancel = closeSearch)
            else -> NotesTitleHeader(showsSearch = !state.isLibraryEmpty(), onSearch = { isSearchOpen = true })
        }
        if (!isSearching && !state.isLibraryEmpty() && state.isNotesLoaded) FolderTabs(state = state, viewModel = viewModel)
        NotesListBody(
            state = state,
            viewModel = viewModel,
            row = NoteRowContext(
                selection = selection,
                writesSummary = state.isSummaryEnabled,
                openNoteId = openNoteId,
                isSearching = isSearching,
                onOpen = { id -> onNoteClick(id, state.searchQuery.isNotEmpty()) },
            ),
            modifier = Modifier.weight(1f),
        )
        when {
            selection.isActive -> SelectionFooter(selection = selection)
            !isSearching -> bottomBar()
        }
    }
}

@Composable
private fun NotesTitleHeader(showsSearch: Boolean, onSearch: () -> Unit) {
    LargeTitleHeader(title = stringResource(R.string.notes_title)) {
        if (showsSearch) {
            BarIconButton(
                icon = OffhandIcons.Search,
                contentDescription = stringResource(R.string.notes_search_placeholder),
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun NotesListBody(state: NotesUiState, viewModel: NotesViewModel, row: NoteRowContext, modifier: Modifier) {
    when {
        !state.isNotesLoaded -> NotesLoadingState(modifier = modifier)
        state.isLibraryEmpty() -> Column(modifier = modifier.fillMaxWidth()) {
            ModelPreparationCard(state.modelPreparation, viewModel::onDownloadOnMobileDataClicked, Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp))
            EmptyFilterState(kind = EmptyNotesKind.LIBRARY, modifier = Modifier.weight(1f))
        }
        else -> NotesList(state = state, viewModel = viewModel, row = row, modifier = modifier)
    }
}

@Composable
private fun NotesList(state: NotesUiState, viewModel: NotesViewModel, row: NoteRowContext, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.sections.firstOrNull()?.notes?.firstOrNull()?.id) {
        if (listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0)
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = MODEL_CARD_KEY, contentType = "model") {
            ModelPreparationCard(state.modelPreparation, viewModel::onDownloadOnMobileDataClicked, Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
        }
        if (row.isSearching) searchResultsLabel(count = state.sections.sumOf { it.notes.size })
        if (state.sections.isEmpty()) {
            item(key = EMPTY_ITEM_KEY, contentType = "empty") {
                EmptyFilterState(kind = state.emptyKind(row.isSearching), modifier = Modifier.fillParentMaxHeight(EMPTY_STATE_HEIGHT_FRACTION))
            }
        }
        noteSections(state = state, viewModel = viewModel, row = row)
        if (row.isSearching && state.sections.isNotEmpty()) searchHint()
    }
}

private fun LazyListScope.noteSections(state: NotesUiState, viewModel: NotesViewModel, row: NoteRowContext) {
    state.sections.forEach { section ->
        if (!row.isSearching) {
            item(key = section.dayLabel.headerKey(), contentType = "header") {
                SectionLabel(text = section.dayLabel.text(), modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 4.dp))
            }
        }
        items(section.notes, key = { it.id }) { note ->
            NoteListItem(
                note = note,
                isWaitingForAi = state.isWaitingForAi,
                progressPercent = state.noteProgress[note.id],
                context = row,
                onDeleteRequested = { viewModel.onDeleteRequested(note.id) },
                onMoveRequested = { viewModel.onMoveToFolderRequested(note.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

private fun LazyListScope.searchResultsLabel(count: Int) {
    item(key = SEARCH_COUNT_KEY, contentType = "header") {
        SectionLabel(
            text = pluralStringResource(R.plurals.notes_search_count, count, count),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
        )
    }
}

private fun LazyListScope.searchHint() {
    item(key = SEARCH_HINT_KEY, contentType = "hint") {
        Text(
            text = stringResource(R.string.notes_search_hint),
            style = OffhandTheme.type.caption,
            color = OffhandTheme.colors.ink3,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
        )
    }
}

@Composable
private fun NotesLoadingState(modifier: Modifier) {
    val description = stringResource(R.string.notes_loading_description)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 22.dp)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(LOADING_ROW_COUNT) {
            Skeleton(lineWidths = LOADING_ROW_LINES)
            Hairline()
        }
    }
}

private enum class EmptyNotesKind(val titleRes: Int, val hintRes: Int?) {
    FOLDER(R.string.notes_folder_empty_state, null),
    UNSORTED(R.string.notes_unsorted_empty_title, R.string.notes_unsorted_empty_hint),
    SEARCH(R.string.notes_search_empty_state, null),
    LIBRARY(R.string.notes_empty_title, R.string.notes_empty_hint),
}

private fun NotesUiState.emptyKind(isSearching: Boolean): EmptyNotesKind = when {
    isSearching -> EmptyNotesKind.SEARCH
    selectedFolderId != null -> EmptyNotesKind.FOLDER
    isUnsortedShown -> EmptyNotesKind.UNSORTED
    else -> EmptyNotesKind.LIBRARY
}

@Composable
private fun EmptyFilterState(kind: EmptyNotesKind, modifier: Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        EmptyState(title = stringResource(kind.titleRes), body = kind.hintRes?.let { stringResource(it) })
    }
}

internal fun NotesUiState.isLibraryEmpty(): Boolean =
    sections.isEmpty() && folders.isEmpty() && searchQuery.isBlank() && selectedFolderId == null

private fun NoteDayLabelUi.headerKey(): String = when (this) {
    NoteDayLabelUi.Today -> "header-today"
    NoteDayLabelUi.Yesterday -> "header-yesterday"
    is NoteDayLabelUi.Date -> "header-$text"
}

@Composable
internal fun NoteDayLabelUi.text(): String = when (this) {
    NoteDayLabelUi.Today -> stringResource(R.string.notes_day_today)
    NoteDayLabelUi.Yesterday -> stringResource(R.string.notes_day_yesterday)
    is NoteDayLabelUi.Date -> text
}

private const val MODEL_CARD_KEY = "model-card"
private const val EMPTY_ITEM_KEY = "empty"
private const val SEARCH_COUNT_KEY = "search-count"
private const val SEARCH_HINT_KEY = "search-hint"
private const val EMPTY_STATE_HEIGHT_FRACTION = 0.7f
private const val LOADING_ROW_COUNT = 4
private val LOADING_ROW_LINES = listOf(0.55f, 0.9f, 0.3f)
