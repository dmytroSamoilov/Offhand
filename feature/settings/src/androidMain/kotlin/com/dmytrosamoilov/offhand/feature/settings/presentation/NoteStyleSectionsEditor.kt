package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SegmentedControl
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.component.FieldLines
import com.dmytrosamoilov.offhand.core.ui.component.LabeledTextField
import com.dmytrosamoilov.offhand.core.ui.component.ReadableSectionLabel
import com.dmytrosamoilov.offhand.feature.settings.R
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleFieldError
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleSectionsError
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidator

@Composable
internal fun SectionsEditor(state: NoteStyleEditorUiState, viewModel: NoteStyleEditorViewModel) {
    ReadableSectionLabel(
        text = stringResource(R.string.settings_note_style_editor_sections),
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.sections.forEachIndexed { index, section ->
            SectionCard(
                position = SectionPosition(index = index, count = state.sections.size),
                section = section,
                headingError = state.errors.headings[index],
                viewModel = viewModel,
            )
        }
        if (state.sections.isEmpty()) Note(text = stringResource(R.string.settings_note_style_editor_no_sections))
        SectionsErrorText(error = state.errors.sections)
        PlainTextButton(
            text = stringResource(R.string.settings_note_style_editor_add_section),
            onClick = viewModel::onSectionAdded,
            enabled = state.sections.size < NoteStyleValidator.MAX_SECTIONS,
            icon = OffhandIcons.Plus,
        )
    }
}

private data class SectionPosition(val index: Int, val count: Int) {
    val isFirst: Boolean get() = index == 0
    val isLast: Boolean get() = index == count - 1
}

@Composable
private fun SectionCard(
    position: SectionPosition,
    section: SectionDraftUi,
    headingError: NoteStyleFieldError?,
    viewModel: NoteStyleEditorViewModel,
) {
    val index = position.index
    SurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCardHeader(position = position, viewModel = viewModel)
            LabeledTextField(
                label = stringResource(R.string.settings_note_style_editor_heading),
                value = section.heading,
                onValueChange = { viewModel.onSectionHeadingChanged(index, it) },
                error = fieldErrorText(headingError, NoteStyleValidator.MAX_HEADING_LENGTH),
            )
            FormatChoice(selected = section.format, onSelected = { viewModel.onSectionFormatChanged(index, it) })
            LabeledTextField(
                label = stringResource(R.string.settings_note_style_editor_guidance),
                value = section.guidance,
                onValueChange = { viewModel.onSectionGuidanceChanged(index, it) },
                placeholder = stringResource(R.string.settings_note_style_editor_guidance_hint),
                lines = FieldLines(min = 2, max = NoteStyleLimits.GUIDANCE_VISIBLE_LINES),
            )
            if (section.guidance.length > NoteStyleLimits.GUIDANCE_WARNING_LENGTH) {
                Note(text = stringResource(R.string.settings_note_style_editor_guidance_warning))
            }
        }
    }
}

@Composable
private fun SectionCardHeader(position: SectionPosition, viewModel: NoteStyleEditorViewModel) {
    val index = position.index
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.settings_note_style_editor_section_title, index + 1),
            style = OffhandTheme.type.caption,
            fontWeight = FontWeight.SemiBold,
            color = OffhandTheme.colors.ink2,
            modifier = Modifier.weight(1f),
        )
        BarIconButton(
            icon = OffhandIcons.ChevronDown,
            contentDescription = stringResource(R.string.settings_note_style_editor_move_up),
            onClick = { viewModel.onSectionMoved(index, index - 1) },
            enabled = !position.isFirst,
            modifier = Modifier.rotate(UPSIDE_DOWN),
        )
        BarIconButton(
            icon = OffhandIcons.ChevronDown,
            contentDescription = stringResource(R.string.settings_note_style_editor_move_down),
            onClick = { viewModel.onSectionMoved(index, index + 1) },
            enabled = !position.isLast,
        )
        BarIconButton(
            icon = OffhandIcons.Close,
            contentDescription = stringResource(R.string.settings_note_style_editor_remove_section),
            onClick = { viewModel.onSectionRemoved(index) },
        )
    }
}

@Composable
private fun FormatChoice(selected: SectionFormat, onSelected: (SectionFormat) -> Unit) {
    val haptics = haptics()
    SegmentedControl(
        options = SectionFormat.entries.map { stringResource(it.labelRes()) },
        selectedIndex = SectionFormat.entries.indexOf(selected),
        onSelect = { index ->
            haptics.tick()
            onSelected(SectionFormat.entries[index])
        },
        compact = true,
    )
}

private fun SectionFormat.labelRes(): Int = when (this) {
    SectionFormat.SENTENCES -> R.string.settings_note_style_editor_format_sentences
    SectionFormat.BULLETS -> R.string.settings_note_style_editor_format_bullets
    SectionFormat.FREE -> R.string.settings_note_style_editor_format_free
}

@Composable
private fun SectionsErrorText(error: NoteStyleSectionsError?) {
    val message = when (error) {
        null -> return
        NoteStyleSectionsError.TOO_MANY ->
            stringResource(R.string.settings_note_style_error_sections_too_many, NoteStyleValidator.MAX_SECTIONS)
    }
    Text(text = message, style = OffhandTheme.type.caption, color = OffhandTheme.colors.danger)
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = OffhandTheme.type.caption, color = OffhandTheme.colors.ink2)
}

private const val UPSIDE_DOWN = 180f
