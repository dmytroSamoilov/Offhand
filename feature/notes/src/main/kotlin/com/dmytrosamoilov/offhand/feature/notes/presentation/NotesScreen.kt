package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.designsystem.component.CollapsibleCard
import com.dmytrosamoilov.offhand.core.designsystem.component.MarkdownText
import com.dmytrosamoilov.offhand.core.designsystem.component.MorphingLoadingIndicator
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.notes.R
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotesScreen(
    onNewRecording: () -> Unit,
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val paneScope = rememberCoroutineScope()

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

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    NotesListPane(
                        sections = state.sections,
                        modelPreparation = state.modelPreparation,
                        onNoteClick = { id ->
                            viewModel.onNoteSelected(id)
                            paneScope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                            }
                        },
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

    if (state.isDeleteConfirmationVisible) {
        DeleteConfirmationDialog(
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }

    if (state.isRetranscribeConfirmationVisible) {
        RetranscribeConfirmationDialog(
            onConfirm = viewModel::onRetranscribeConfirmed,
            onDismiss = viewModel::onRetranscribeDismissed,
        )
    }
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
    modelPreparation: ModelPreparationUi?,
    onNoteClick: (Long) -> Unit,
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
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.notes_title)) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewRecording,
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
                .padding(innerPadding),
        ) {
            ModelPreparationBanner(preparation = modelPreparation)
            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.notes_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sections.forEach { section ->
                        item(key = section.dayLabel.headerKey(), contentType = "header") {
                            SectionHeader(dayLabel = section.dayLabel)
                        }
                        items(section.notes, key = { it.id }) { note ->
                            NoteCard(note = note, onClick = { onNoteClick(note.id) })
                        }
                    }
                }
            }
        }
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
                        text = note.cardTitle(),
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
            if (note.status == NoteStatusUi.READY && note.durationText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                NoteCardMetadata(durationText = note.durationText)
            }
        }
    }
}

@Composable
private fun NoteCardMetadata(durationText: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MetadataText(text = durationText)
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
        NoteStatusUi.FAILED -> MaterialTheme.colorScheme.errorContainer
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
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun NoteCardUi.cardTitle(): String = when (status) {
    NoteStatusUi.PROCESSING -> stringResource(R.string.notes_processing_title)
    NoteStatusUi.FAILED -> stringResource(R.string.notes_failed_title)
    NoteStatusUi.READY -> title
}

@Composable
private fun NoteCardUi.cardPreview(): String = when (status) {
    NoteStatusUi.PROCESSING -> stringResource(R.string.notes_processing_preview)
    NoteStatusUi.FAILED -> stringResource(R.string.notes_failed_preview)
    NoteStatusUi.READY -> preview
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
            showMetrics = state.isDeveloperMode,
            progressPercent = state.noteProgress[selected.id],
            onBack = viewModel::onDetailClosed,
            onEdit = viewModel::onEditStarted,
            onDeleteRequested = viewModel::onDeleteRequested,
            onPlayPause = viewModel::onPlayPauseClicked,
            onSeek = viewModel::onSeekRequested,
            onRetryTranscription = viewModel::onRetryTranscriptionRequested,
            onRetranscribeRequested = viewModel::onRetranscribeRequested,
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
    showMetrics: Boolean,
    progressPercent: Int?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequested: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onRetryTranscription: () -> Unit,
    onRetranscribeRequested: () -> Unit,
) {
    Scaffold(
        topBar = {
            NoteDetailTopBar(
                showEditActions = note.status == NoteStatusUi.READY,
                note = note,
                onBack = onBack,
                onEdit = onEdit,
                onDeleteRequested = onDeleteRequested,
                onRetranscribeRequested = onRetranscribeRequested,
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        NoteDetailContent(
            note = note,
            playback = playback,
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
    onDeleteRequested: () -> Unit,
    onRetranscribeRequested: () -> Unit,
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
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.notes_edit_description),
                    )
                }
                ShareNoteButton(note = note)
                NoteOverflowMenu(
                    showRetranscribe = note.hasAudio,
                    onRetranscribeRequested = onRetranscribeRequested,
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
private fun ShareNoteButton(note: NoteDetailUi) {
    val context = LocalContext.current
    IconButton(
        onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, buildShareText(note))
            context.startActivity(Intent.createChooser(shareIntent, null))
        },
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = stringResource(R.string.notes_share_description),
        )
    }
}

@Composable
private fun NoteOverflowMenu(
    showRetranscribe: Boolean,
    onRetranscribeRequested: () -> Unit,
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
        when (note.status) {
            NoteStatusUi.PROCESSING -> DetailStatusCard(
                text = stringResource(R.string.notes_processing_detail),
                percent = progressPercent,
            )
            NoteStatusUi.FAILED -> FailedDetailCard(
                hasAudio = note.hasAudio,
                onRetryTranscription = onRetryTranscription,
            )
            NoteStatusUi.READY -> key(note.id) {
                CollapsibleCard(
                    title = stringResource(R.string.notes_overview_heading),
                    labelContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    initiallyExpanded = true,
                ) {
                    MarkdownText(markdown = note.body)
                }
                if (note.transcript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CollapsibleCard(
                        title = stringResource(R.string.notes_transcript_heading),
                        labelContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        MarkdownText(markdown = note.transcript)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustBadges() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrustBadge(
            icon = Icons.Filled.CloudOff,
            label = stringResource(R.string.notes_badge_offline),
        )
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
    NoteStatusUi.PROCESSING -> stringResource(R.string.notes_processing_title)
    NoteStatusUi.FAILED -> stringResource(R.string.notes_failed_title)
    NoteStatusUi.READY -> title
}

@Composable
private fun FailedDetailCard(
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
                text = stringResource(R.string.notes_failed_detail),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasAudio) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetryTranscription) {
                    Text(text = stringResource(R.string.notes_transcribe_button))
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
    Card(modifier = Modifier.fillMaxWidth(), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onPlayPause,
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

private fun buildShareText(note: NoteDetailUi): String = buildString {
    appendLine(note.title)
    appendLine()
    append(note.body)
    if (note.transcript.isNotBlank()) {
        appendLine()
        appendLine()
        append(note.transcript)
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
