package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.Hairline
import com.dmytrosamoilov.offhand.core.designsystem.component.MetaText
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.RecordPill
import com.dmytrosamoilov.offhand.core.designsystem.component.Skeleton
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.onboarding.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onRecordFirstNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        OnboardingContent(
            state = state,
            onRecordFirstNote = {
                onRecordFirstNote()
                viewModel.onRecordFirstNoteClicked()
            },
            onLookAround = viewModel::onLookAroundClicked,
        )
    }
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    onRecordFirstNote: () -> Unit,
    onLookAround: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffhandTheme.colors.ground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth()
            .widthIn(max = MAX_CONTENT_WIDTH)
            .padding(start = OffhandSpacing.screen, end = OffhandSpacing.screen, top = 12.dp, bottom = 28.dp),
    ) {
        Wordmark()
        when (state.step) {
            OnboardingStep.DEVICE_CHECK -> Skeleton(modifier = Modifier.padding(top = 56.dp))
            OnboardingStep.DEVICE_INCOMPATIBLE -> DeviceIncompatible(state.deviceSpecs)
            OnboardingStep.WELCOME -> Welcome(onRecordFirstNote = onRecordFirstNote, onLookAround = onLookAround)
        }
    }
}

@Composable
private fun Wordmark() {
    Text(
        text = stringResource(R.string.onboarding_wordmark),
        style = OffhandTheme.type.heading.copy(fontSize = OffhandTheme.type.rowTitle.fontSize),
        color = OffhandTheme.colors.ink,
        modifier = Modifier.heightIn(min = OffhandSpacing.touchTarget).padding(top = 10.dp),
    )
}

@Composable
private fun ColumnScope.Welcome(onRecordFirstNote: () -> Unit, onLookAround: () -> Unit) {
    Headline(text = stringResource(R.string.onboarding_welcome_title))
    Column(modifier = Modifier.padding(top = OffhandSpacing.screen)) {
        StageRow(R.string.onboarding_step_recorded, showDivider = true)
        StageRow(R.string.onboarding_step_transcribed, showDivider = true)
        StageRow(R.string.onboarding_step_summarised, showDivider = false)
    }
    Text(
        text = stringResource(R.string.onboarding_privacy_line),
        style = OffhandTheme.type.bodySmall,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 16.dp),
    )
    Spacer(Modifier.weight(1f).heightIn(min = 40.dp))
    RecordPill(
        text = stringResource(R.string.onboarding_record_first_note),
        onClick = onRecordFirstNote,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(R.string.onboarding_microphone_hint),
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
    PlainTextButton(
        text = stringResource(R.string.onboarding_look_around),
        onClick = onLookAround,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}

@Composable
private fun StageRow(labelRes: Int, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(labelRes), style = OffhandTheme.type.body, color = OffhandTheme.colors.ink)
            MetaText(text = stringResource(R.string.onboarding_on_your_phone))
        }
        if (showDivider) Hairline()
    }
}

@Composable
private fun Headline(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.display,
        color = OffhandTheme.colors.ink,
        modifier = Modifier.padding(top = 56.dp).semantics { heading() },
    )
}

@Composable
private fun DeviceIncompatible(specs: DeviceSpecsUi?) {
    Headline(text = stringResource(R.string.onboarding_incompatible_title))
    Text(
        text = stringResource(R.string.onboarding_incompatible_body),
        style = OffhandTheme.type.body,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 12.dp),
    )
    specs?.let { SpecRows(it) }
}

@Composable
private fun SpecRows(specs: DeviceSpecsUi) {
    Column(modifier = Modifier.padding(top = OffhandSpacing.screen)) {
        SpecRow(
            label = stringResource(R.string.onboarding_incompatible_ram_label),
            value = stringResource(R.string.onboarding_incompatible_ram_value, specs.totalRamGb, specs.requiredRamGb),
            isSatisfied = specs.isRamSatisfied,
        )
        Hairline()
        SpecRow(
            label = stringResource(R.string.onboarding_incompatible_cores_label),
            value = stringResource(R.string.onboarding_incompatible_cores_value, specs.cpuCores, specs.requiredCpuCores),
            isSatisfied = specs.isCoresSatisfied,
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String, isSatisfied: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = OffhandTheme.type.body, color = OffhandTheme.colors.ink, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = OffhandTheme.type.number,
            color = if (isSatisfied) OffhandTheme.colors.ink2 else OffhandTheme.colors.danger,
        )
    }
}

@Preview
@Composable
private fun WelcomePreview() {
    OffhandTheme {
        OnboardingContent(state = OnboardingUiState(step = OnboardingStep.WELCOME), onRecordFirstNote = {}, onLookAround = {})
    }
}

private val MAX_CONTENT_WIDTH = 560.dp
