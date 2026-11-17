package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ScreenTitle
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NewStyleButton
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleList
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
        SettingsPage(
            topBar = {
                BackTopBar(
                    backDescription = stringResource(R.string.settings_note_style_editor_back),
                    onBack = onBack,
                    trailing = { NewStyleButton(onClick = onCreateStyle) },
                )
            },
        ) {
            ScreenTitle(
                title = stringResource(R.string.settings_note_styles_title),
                subtitle = stringResource(R.string.settings_note_style_note),
                modifier = Modifier.padding(top = 8.dp),
            )
            NoteStyleList(
                selected = state.selected,
                customStyles = state.customStyles.map { NoteStyleChoice(id = it.id, name = it.name, description = it.description) },
                isProStylesUnlocked = state.isUnlocked,
                onSelected = viewModel::onStyleSelected,
                customStyleActions = { style ->
                    PlainTextButton(text = stringResource(R.string.settings_note_styles_edit), onClick = { onEditStyle(style.id) })
                },
            )
        }
    }
}
