package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.NewStyleButton
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleList
import com.dmytrosamoilov.offhand.feature.settings.R
import org.koin.androidx.compose.koinViewModel

// The same list as the restyle sheet: picking a row sets the default for new
// recordings, custom rows open the editor, where deleting lives.
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
            topBar = { NoteStylesTopBar(onBack = onBack, onCreateStyle = onCreateStyle) },
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_note_style_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NoteStyleList(
                    selected = state.selected,
                    customStyles = state.customStyles.map { NoteStyleChoice(id = it.id, name = it.name, description = it.description) },
                    isProStylesUnlocked = state.isUnlocked,
                    onSelected = viewModel::onStyleSelected,
                    customStyleActions = { style ->
                        TextButton(onClick = { onEditStyle(style.id) }) {
                            Text(text = stringResource(R.string.settings_note_styles_edit))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NoteStylesTopBar(onBack: () -> Unit, onCreateStyle: () -> Unit) {
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
        actions = { NewStyleButton(onClick = onCreateStyle, modifier = Modifier.padding(end = 8.dp)) },
    )
}
