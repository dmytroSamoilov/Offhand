package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.ChoiceRow
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandSheet
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

// Confirm and every other way of closing the sheet end in the same place:
// the selected rows are the answer.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UsageSurveySheet(
    selection: Set<UsageSurveyAnswerUi>,
    onToggled: (UsageSurveyAnswerUi) -> Unit,
    onClosed: () -> Unit,
) {
    OffhandSheet(onDismissRequest = onClosed, title = stringResource(R.string.notes_usage_survey_title)) {
        Text(
            text = stringResource(R.string.notes_usage_survey_body),
            style = OffhandTheme.type.dialogBody,
            color = OffhandTheme.colors.ink2,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        UsageSurveyAnswerUi.entries.forEach { answer ->
            ChoiceRow(
                title = stringResource(answer.labelRes()),
                isSelected = answer in selection,
                onClick = { onToggled(answer) },
                showDivider = answer != UsageSurveyAnswerUi.entries.last(),
                role = Role.Checkbox,
            )
        }
        PrimaryButton(
            text = stringResource(R.string.notes_usage_survey_confirm),
            onClick = onClosed,
            enabled = selection.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )
    }
}

private fun UsageSurveyAnswerUi.labelRes(): Int = when (this) {
    UsageSurveyAnswerUi.MEETINGS -> R.string.notes_usage_survey_meetings
    UsageSurveyAnswerUi.THOUGHTS -> R.string.notes_usage_survey_thoughts
    UsageSurveyAnswerUi.LECTURES -> R.string.notes_usage_survey_lectures
    UsageSurveyAnswerUi.INTERVIEWS -> R.string.notes_usage_survey_interviews
    UsageSurveyAnswerUi.OTHER -> R.string.notes_usage_survey_other
}
