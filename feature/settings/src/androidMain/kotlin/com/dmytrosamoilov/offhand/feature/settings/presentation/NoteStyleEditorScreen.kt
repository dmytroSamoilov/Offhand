package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.core.designsystem.theme.extendedColors
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.settings.R
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleFieldError
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleSectionsError
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NoteStyleEditorScreen(
    styleId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteStyleEditorViewModel = koinViewModel(parameters = { parametersOf(styleId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    state.describe?.let { describe ->
        DescribeStyleDialog(
            state = describe,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onBuild = viewModel::onDraftRequested,
            onDismiss = viewModel::onDescribeDismissed,
        )
    }
    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Scaffold(
            topBar = {
                EditorTopBar(isNew = state.isNew, onBack = onBack, onSave = viewModel::onSaveRequested)
            },
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            EditorContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun EditorTopBar(isNew: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    AppTopBar(
        title = stringResource(
            if (isNew) R.string.settings_note_style_editor_new_title else R.string.settings_note_style_editor_edit_title,
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_note_style_editor_back),
                )
            }
        },
        actions = {
            TextButton(onClick = onSave) { Text(text = stringResource(R.string.settings_note_style_editor_save)) }
        },
    )
}

@Composable
private fun EditorContent(
    state: NoteStyleEditorUiState,
    viewModel: NoteStyleEditorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DescribeButton(onClick = viewModel::onDescribeRequested)
        NameField(value = state.name, error = state.errors.name, onValueChange = viewModel::onNameChanged)
        KindField(value = state.noteKind, error = state.errors.noteKind, onValueChange = viewModel::onNoteKindChanged)
        LanguageChoice(selected = state.language, onSelected = viewModel::onLanguageChanged)
        SectionsEditor(state = state, viewModel = viewModel)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DescribeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
        Spacer(modifier = Modifier.padding(6.dp))
        Text(
            text = stringResource(R.string.settings_note_style_describe_button),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun NameField(value: String, error: NoteStyleFieldError?, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(R.string.settings_note_style_editor_name)) },
        isError = error != null,
        supportingText = fieldErrorText(error, NoteStyleValidator.MAX_NAME_LENGTH),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun KindField(value: String, error: NoteStyleFieldError?, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(R.string.settings_note_style_editor_kind)) },
        placeholder = { Text(text = stringResource(R.string.settings_note_style_editor_kind_hint)) },
        isError = error != null,
        supportingText = fieldErrorText(error, NoteStyleValidator.MAX_KIND_LENGTH)
            ?: { Text(text = stringResource(R.string.settings_note_style_editor_kind_help)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun fieldErrorText(error: NoteStyleFieldError?, maxLength: Int): (@Composable () -> Unit)? {
    val message = when (error) {
        null -> return null
        NoteStyleFieldError.BLANK -> stringResource(R.string.settings_note_style_error_blank)
        NoteStyleFieldError.TOO_LONG -> stringResource(R.string.settings_note_style_error_too_long, maxLength)
        NoteStyleFieldError.DUPLICATE -> stringResource(R.string.settings_note_style_error_duplicate)
    }
    return { Text(text = message) }
}

@Composable
private fun LanguageChoice(selected: NoteStyleLanguage, onSelected: (NoteStyleLanguage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_note_style_editor_language),
            style = MaterialTheme.typography.titleSmall,
        )
        val haptics = haptics()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            NoteStyleLanguage.entries.forEachIndexed { index, language ->
                SegmentedButton(
                    selected = language == selected,
                    onClick = {
                        haptics.tick()
                        onSelected(language)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = NoteStyleLanguage.entries.size),
                ) {
                    Text(text = stringResource(language.labelRes()))
                }
            }
        }
    }
}

private fun NoteStyleLanguage.labelRes(): Int = when (this) {
    NoteStyleLanguage.RECORDING -> R.string.settings_note_style_editor_language_recording
    NoteStyleLanguage.ENGLISH -> R.string.settings_note_style_editor_language_english
}

@Composable
private fun SectionsEditor(state: NoteStyleEditorUiState, viewModel: NoteStyleEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.settings_note_style_editor_sections),
            style = MaterialTheme.typography.titleSmall,
        )
        state.sections.forEachIndexed { index, section ->
            SectionCard(
                index = index,
                count = state.sections.size,
                section = section,
                headingError = state.errors.headings[index],
                viewModel = viewModel,
            )
        }
        if (state.sections.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_note_style_editor_no_sections),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionsErrorText(error = state.errors.sections)
        OutlinedButton(
            onClick = viewModel::onSectionAdded,
            enabled = state.sections.size < NoteStyleValidator.MAX_SECTIONS,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text = stringResource(R.string.settings_note_style_editor_add_section))
        }
    }
}

@Composable
private fun SectionsErrorText(error: NoteStyleSectionsError?) {
    val message = when (error) {
        null -> return
        NoteStyleSectionsError.TOO_MANY ->
            stringResource(R.string.settings_note_style_error_sections_too_many, NoteStyleValidator.MAX_SECTIONS)
    }
    Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun SectionCard(
    index: Int,
    count: Int,
    section: SectionDraftUi,
    headingError: NoteStyleFieldError?,
    viewModel: NoteStyleEditorViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionCardHeader(index = index, count = count, viewModel = viewModel)
            OutlinedTextField(
                value = section.heading,
                onValueChange = { viewModel.onSectionHeadingChanged(index, it) },
                label = { Text(text = stringResource(R.string.settings_note_style_editor_heading)) },
                isError = headingError != null,
                supportingText = fieldErrorText(headingError, NoteStyleValidator.MAX_HEADING_LENGTH),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = section.guidance,
                onValueChange = { viewModel.onSectionGuidanceChanged(index, it) },
                label = { Text(text = stringResource(R.string.settings_note_style_editor_guidance)) },
                placeholder = { Text(text = stringResource(R.string.settings_note_style_editor_guidance_hint)) },
                supportingText = guidanceWarning(section.guidance),
                minLines = 2,
                maxLines = NoteStyleLimits.GUIDANCE_VISIBLE_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
            FormatChoice(selected = section.format, onSelected = { viewModel.onSectionFormatChanged(index, it) })
        }
    }
}

@Composable
private fun SectionCardHeader(index: Int, count: Int, viewModel: NoteStyleEditorViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.settings_note_style_editor_section_title, index + 1),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { viewModel.onSectionMoved(index, index - 1) }, enabled = index > 0) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.settings_note_style_editor_move_up),
            )
        }
        IconButton(onClick = { viewModel.onSectionMoved(index, index + 1) }, enabled = index < count - 1) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.settings_note_style_editor_move_down),
            )
        }
        IconButton(onClick = { viewModel.onSectionRemoved(index) }) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.settings_note_style_editor_remove_section),
            )
        }
    }
}

@Composable
private fun FormatChoice(selected: SectionFormat, onSelected: (SectionFormat) -> Unit) {
    val haptics = haptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SectionFormat.entries.forEachIndexed { index, format ->
            SegmentedButton(
                selected = format == selected,
                onClick = {
                    haptics.tick()
                    onSelected(format)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = SectionFormat.entries.size),
            ) {
                Text(text = stringResource(format.labelRes()))
            }
        }
    }
}

private fun SectionFormat.labelRes(): Int = when (this) {
    SectionFormat.SENTENCES -> R.string.settings_note_style_editor_format_sentences
    SectionFormat.BULLETS -> R.string.settings_note_style_editor_format_bullets
    SectionFormat.FREE -> R.string.settings_note_style_editor_format_free
}

// Long guidance is allowed; past the threshold the user is told the model
// may not cope with it, and the choice stays theirs.
private fun guidanceWarning(guidance: String): (@Composable () -> Unit)? {
    if (guidance.length <= NoteStyleLimits.GUIDANCE_WARNING_LENGTH) return null
    return {
        Text(
            text = stringResource(R.string.settings_note_style_editor_guidance_warning),
            color = MaterialTheme.extendedColors.onWarningContainer,
            modifier = Modifier
                .background(MaterialTheme.extendedColors.warningContainer, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
