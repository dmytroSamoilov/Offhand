package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.CustomNoteStyleIcon
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.feature.settings.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteStylesScreen(
    onBack: () -> Unit,
    onCreateStyle: () -> Unit,
    onEditStyle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteStylesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Scaffold(
            topBar = { NoteStylesTopBar(onBack = onBack) },
            floatingActionButton = {
                NewStyleButton(isUnlocked = state.isUnlocked, onClick = onCreateStyle)
            },
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            NoteStylesList(
                state = state,
                onEditStyle = onEditStyle,
                onDeleteStyle = viewModel::onDeleteRequested,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
    if (state.pendingDeleteId != null) {
        DeleteStyleDialog(onConfirm = viewModel::onDeleteConfirmed, onDismiss = viewModel::onDeleteDismissed)
    }
}

@Composable
private fun NoteStylesTopBar(onBack: () -> Unit) {
    AppTopBar(
        title = stringResource(R.string.settings_note_styles_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_note_style_editor_back),
                )
            }
        },
    )
}

@Composable
private fun NewStyleButton(isUnlocked: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.settings_note_styles_new)
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { if (isUnlocked) Icon(imageVector = Icons.Filled.Add, contentDescription = null) else ProCrown(size = 22.dp) },
        text = { Text(text = label) },
        modifier = Modifier.semantics { contentDescription = label },
    )
}

@Composable
private fun NoteStylesList(
    state: NoteStylesUiState,
    onEditStyle: (Long) -> Unit,
    onDeleteStyle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader(text = stringResource(R.string.settings_note_styles_built_in)) }
        items(NotePresetOption.entries) { option -> BuiltInStyleRow(option) }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(text = stringResource(R.string.settings_note_styles_custom))
        }
        if (state.customStyles.isEmpty()) {
            item { EmptyStylesHint() }
        }
        items(state.customStyles, key = { it.id }) { style ->
            CustomStyleRow(style = style, onClick = { onEditStyle(style.id) }, onDelete = { onDeleteStyle(style.id) })
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun BuiltInStyleRow(option: NotePresetOption) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = option.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            StyleTexts(title = stringResource(option.labelRes), description = stringResource(option.descriptionRes))
        }
    }
}

@Composable
private fun CustomStyleRow(style: CustomStyleOptionUi, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = CustomNoteStyleIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            StyleTexts(title = style.name, description = style.description)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.settings_note_styles_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RowScope.StyleTexts(title: String, description: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyStylesHint() {
    Text(
        text = stringResource(R.string.settings_note_styles_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun DeleteStyleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_note_styles_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.settings_note_styles_delete_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.settings_note_styles_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_delete_model_dialog_cancel))
            }
        },
    )
}
