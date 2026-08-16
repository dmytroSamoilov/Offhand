package com.dmytrosamoilov.offhand.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.designsystem.component.RoundedCheckbox
import com.dmytrosamoilov.offhand.core.ui.R

enum class NotePresetOption(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
) {
    SUMMARY(
        labelRes = R.string.core_ui_note_preset_summary,
        descriptionRes = R.string.core_ui_note_preset_summary_description,
        icon = Icons.Filled.Summarize,
    ),
    MEETING(
        labelRes = R.string.core_ui_note_preset_meeting,
        descriptionRes = R.string.core_ui_note_preset_meeting_description,
        icon = Icons.Filled.Groups,
    ),
    VISIT(
        labelRes = R.string.core_ui_note_preset_visit,
        descriptionRes = R.string.core_ui_note_preset_visit_description,
        icon = Icons.Filled.FactCheck,
    ),
    LEGAL(
        labelRes = R.string.core_ui_note_preset_legal,
        descriptionRes = R.string.core_ui_note_preset_legal_description,
        icon = Icons.Filled.Gavel,
    ),
}

fun NotePreset.toUi(): NotePresetOption = when (this) {
    NotePreset.SUMMARY -> NotePresetOption.SUMMARY
    NotePreset.MEETING -> NotePresetOption.MEETING
    NotePreset.VISIT -> NotePresetOption.VISIT
    NotePreset.LEGAL -> NotePresetOption.LEGAL
}

fun NotePresetOption.toDomain(): NotePreset = when (this) {
    NotePresetOption.SUMMARY -> NotePreset.SUMMARY
    NotePresetOption.MEETING -> NotePreset.MEETING
    NotePresetOption.VISIT -> NotePreset.VISIT
    NotePresetOption.LEGAL -> NotePreset.LEGAL
}

@Composable
fun NotePresetOptionCard(
    option: NotePresetOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(option.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(option.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            RoundedCheckbox(checked = isSelected)
        }
    }
}
