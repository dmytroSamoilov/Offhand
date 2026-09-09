package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.feature.settings.R

@Composable
internal fun DescribeStyleDialog(
    state: DescribeStyleUi,
    onDescriptionChanged: (String) -> Unit,
    onBuild: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isRunning = state.status == DescribeStatusUi.RUNNING
    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        title = { Text(text = stringResource(R.string.settings_note_style_describe_title)) },
        text = { DescribeStyleBody(state = state, onDescriptionChanged = onDescriptionChanged) },
        confirmButton = {
            TextButton(onClick = onBuild, enabled = !isRunning && state.description.isNotBlank()) {
                Text(text = stringResource(R.string.settings_note_style_describe_build))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRunning) {
                Text(text = stringResource(R.string.settings_delete_model_dialog_cancel))
            }
        },
    )
}

@Composable
private fun DescribeStyleBody(state: DescribeStyleUi, onDescriptionChanged: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.settings_note_style_describe_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = { onDescriptionChanged(it.take(NoteStyleLimits.MAX_DESCRIPTION_LENGTH)) },
            placeholder = {
                Text(
                    text = stringResource(R.string.settings_note_style_describe_hint),
                    color = MaterialTheme.colorScheme.outline,
                )
            },
            enabled = state.status != DescribeStatusUi.RUNNING,
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
        DescribeStatus(status = state.status)
    }
}

@Composable
private fun DescribeStatus(status: DescribeStatusUi) {
    when (status) {
        DescribeStatusUi.IDLE -> Unit
        DescribeStatusUi.RUNNING -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            StatusText(text = stringResource(R.string.settings_note_style_describe_running))
        }
        DescribeStatusUi.MODEL_UNAVAILABLE ->
            StatusText(text = stringResource(R.string.settings_note_style_editor_preview_unavailable), isError = true)
        DescribeStatusUi.FAILED ->
            StatusText(text = stringResource(R.string.settings_note_style_describe_failed), isError = true)
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
