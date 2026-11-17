package com.dmytrosamoilov.offhand.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceRow
import com.dmytrosamoilov.offhand.core.ui.R

enum class NotePresetOption(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {
    SUMMARY(
        labelRes = R.string.core_ui_note_preset_summary,
        descriptionRes = R.string.core_ui_note_preset_summary_description,
    ),
    MEETING(
        labelRes = R.string.core_ui_note_preset_meeting,
        descriptionRes = R.string.core_ui_note_preset_meeting_description,
    ),
    VISIT(
        labelRes = R.string.core_ui_note_preset_visit,
        descriptionRes = R.string.core_ui_note_preset_visit_description,
    ),
    LEGAL(
        labelRes = R.string.core_ui_note_preset_legal,
        descriptionRes = R.string.core_ui_note_preset_legal_description,
    ),
    EMAIL(
        labelRes = R.string.core_ui_note_preset_email,
        descriptionRes = R.string.core_ui_note_preset_email_description,
    ),
}

fun NotePreset.toUi(): NotePresetOption = when (this) {
    NotePreset.SUMMARY -> NotePresetOption.SUMMARY
    NotePreset.MEETING -> NotePresetOption.MEETING
    NotePreset.VISIT -> NotePresetOption.VISIT
    NotePreset.LEGAL -> NotePresetOption.LEGAL
    NotePreset.EMAIL -> NotePresetOption.EMAIL
}

fun NotePresetOption.toDomain(): NotePreset = when (this) {
    NotePresetOption.SUMMARY -> NotePreset.SUMMARY
    NotePresetOption.MEETING -> NotePreset.MEETING
    NotePresetOption.VISIT -> NotePreset.VISIT
    NotePresetOption.LEGAL -> NotePreset.LEGAL
    NotePresetOption.EMAIL -> NotePreset.EMAIL
}

@Composable
fun NotePresetOptionCard(
    option: NotePresetOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProBadge: Boolean = false,
    showDivider: Boolean = true,
) {
    NoteStyleCard(
        title = stringResource(option.labelRes),
        description = stringResource(option.descriptionRes),
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        showProBadge = showProBadge,
        showDivider = showDivider,
    )
}

@Composable
fun NoteStyleCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProBadge: Boolean = false,
    showDivider: Boolean = true,
    trailingActions: @Composable RowScope.() -> Unit = {},
) {
    ChoiceRow(
        title = title,
        subtitle = description.ifBlank { null },
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        showProBadge = showProBadge,
        showDivider = showDivider,
        minHeight = 66.dp,
        trailing = { Row(content = trailingActions) },
    )
}
