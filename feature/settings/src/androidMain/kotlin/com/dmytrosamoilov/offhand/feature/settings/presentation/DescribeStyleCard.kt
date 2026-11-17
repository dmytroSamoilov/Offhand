package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.component.FieldLines
import com.dmytrosamoilov.offhand.core.ui.component.LabeledTextField
import com.dmytrosamoilov.offhand.feature.settings.R

@Composable
internal fun DescribeStyleCard(
    state: DescribeStyleUi,
    onDescriptionChanged: (String) -> Unit,
    onBuild: () -> Unit,
) {
    val isRunning = state.status == DescribeStatusUi.RUNNING
    SurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledTextField(
                label = stringResource(R.string.settings_note_style_describe_button),
                value = state.description,
                onValueChange = { onDescriptionChanged(it.take(NoteStyleLimits.MAX_DESCRIPTION_LENGTH)) },
                placeholder = stringResource(R.string.settings_note_style_describe_hint),
                lines = FieldLines(min = 2, max = DESCRIPTION_LINES),
                enabled = !isRunning,
                filledWithGround = true,
            )
            DescribeStatus(status = state.status)
            SecondaryButton(
                text = stringResource(R.string.settings_note_style_describe_build),
                onClick = onBuild,
                enabled = !isRunning && state.description.isNotBlank(),
                height = 44.dp,
            )
        }
    }
}

@Composable
private fun DescribeStatus(status: DescribeStatusUi) {
    when (status) {
        DescribeStatusUi.IDLE -> Unit
        DescribeStatusUi.RUNNING -> {
            LinearProgressIndicator(
                color = OffhandTheme.colors.accent,
                trackColor = OffhandTheme.colors.hairline,
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            StatusText(text = stringResource(R.string.settings_note_style_describe_running), isError = false)
        }
        DescribeStatusUi.MODEL_UNAVAILABLE ->
            StatusText(text = stringResource(R.string.settings_note_style_describe_unavailable), isError = true)
        DescribeStatusUi.FAILED ->
            StatusText(text = stringResource(R.string.settings_note_style_describe_failed), isError = true)
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean) {
    Text(
        text = text,
        style = OffhandTheme.type.caption,
        color = if (isError) OffhandTheme.colors.danger else OffhandTheme.colors.ink2,
    )
}

private const val DESCRIPTION_LINES = 6
