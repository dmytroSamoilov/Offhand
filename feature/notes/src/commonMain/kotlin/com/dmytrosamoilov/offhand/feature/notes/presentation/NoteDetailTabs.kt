package com.dmytrosamoilov.offhand.feature.notes.presentation

enum class NoteDetailTabUi {
    SUMMARY,
    TRANSCRIPT,
}

internal data class NoteDetailTabState(
    val tab: NoteDetailTabUi = NoteDetailTabUi.SUMMARY,
    val isSummaryReadyMarked: Boolean = false,
) {
    fun onStatusChanged(previous: NoteStatusUi, current: NoteStatusUi): NoteDetailTabState = when {
        current != NoteStatusUi.READY -> copy(isSummaryReadyMarked = false)
        previous != NoteStatusUi.READY && tab == NoteDetailTabUi.TRANSCRIPT -> copy(isSummaryReadyMarked = true)
        else -> this
    }

    fun onTabSelected(selected: NoteDetailTabUi): NoteDetailTabState = NoteDetailTabState(
        tab = selected,
        isSummaryReadyMarked = isSummaryReadyMarked && selected != NoteDetailTabUi.SUMMARY,
    )

    companion object {
        fun openedFor(status: NoteStatusUi): NoteDetailTabState = NoteDetailTabState(
            tab = if (status == NoteStatusUi.PROCESSING) NoteDetailTabUi.TRANSCRIPT else NoteDetailTabUi.SUMMARY,
        )
    }
}

internal fun NotesUiState.detailTabs(): NoteDetailTabState =
    NoteDetailTabState(tab = detailTab, isSummaryReadyMarked = isSummaryReadyMarked)

internal fun NotesUiState.withDetailTabs(tabs: NoteDetailTabState): NotesUiState =
    copy(detailTab = tabs.tab, isSummaryReadyMarked = tabs.isSummaryReadyMarked)
