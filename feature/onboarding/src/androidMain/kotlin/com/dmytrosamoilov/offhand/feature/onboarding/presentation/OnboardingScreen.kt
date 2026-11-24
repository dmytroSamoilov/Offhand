package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.RecordPill
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel
import com.dmytrosamoilov.offhand.core.designsystem.component.WaveGlyph
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
            onContinueWithTranscripts = viewModel::onContinueWithTranscriptsClicked,
            onShareAnalytics = viewModel::onShareAnalyticsClicked,
            onDontShareAnalytics = viewModel::onDontShareAnalyticsClicked,
        )
    }
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    onRecordFirstNote: () -> Unit,
    onLookAround: () -> Unit,
    onContinueWithTranscripts: () -> Unit,
    onShareAnalytics: () -> Unit,
    onDontShareAnalytics: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffhandTheme.colors.ground)
            .systemBarsPadding()
            .wrapContentWidth()
            .widthIn(max = MAX_CONTENT_WIDTH),
    ) {
        StepBody(state = state, modifier = Modifier.weight(1f))
        when (state.step) {
            OnboardingStep.DEVICE_CHECK -> Unit
            OnboardingStep.ANALYTICS_CONSENT -> AnalyticsConsentActions(onShare = onShareAnalytics, onDontShare = onDontShareAnalytics)
            OnboardingStep.DEVICE_INCOMPATIBLE -> DeviceIncompatibleActions()
            OnboardingStep.TRANSCRIPTS_ONLY -> TranscriptsOnlyActions(onContinue = onContinueWithTranscripts)
            OnboardingStep.WELCOME -> WelcomeActions(onRecordFirstNote = onRecordFirstNote, onLookAround = onLookAround)
        }
    }
}

@Composable
private fun StepBody(state: OnboardingUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(remember(state.step) { ScrollState(initial = 0) })
            .padding(start = OffhandSpacing.screen, end = OffhandSpacing.screen, top = 12.dp, bottom = 16.dp),
    ) {
        Wordmark()
        when (state.step) {
            OnboardingStep.DEVICE_CHECK -> Skeleton(modifier = Modifier.padding(top = 56.dp))
            OnboardingStep.ANALYTICS_CONSENT -> AnalyticsConsent()
            OnboardingStep.DEVICE_INCOMPATIBLE -> DeviceIncompatible(state.deviceSpecs)
            OnboardingStep.TRANSCRIPTS_ONLY -> TranscriptsOnly()
            OnboardingStep.WELCOME -> Welcome(isSummaryAvailable = state.isSummaryAvailable)
        }
    }
}

@Composable
private fun StepActions(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = OffhandSpacing.screen, end = OffhandSpacing.screen, top = 12.dp, bottom = 28.dp),
        content = content,
    )
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
private fun Welcome(isSummaryAvailable: Boolean) {
    Headline(text = stringResource(R.string.onboarding_welcome_title))
    Column(modifier = Modifier.padding(top = OffhandSpacing.screen)) {
        StageRow(stringResource(R.string.onboarding_step_recorded), stringResource(R.string.onboarding_on_your_phone))
        Hairline()
        StageRow(stringResource(R.string.onboarding_step_transcribed), stringResource(R.string.onboarding_on_your_phone))
        Hairline()
        StageRow(
            label = stringResource(R.string.onboarding_step_summarised),
            meta = stringResource(if (isSummaryAvailable) R.string.onboarding_on_your_phone else R.string.onboarding_not_here),
            isMuted = !isSummaryAvailable,
        )
    }
    Text(
        text = stringResource(R.string.onboarding_privacy_line),
        style = OffhandTheme.type.bodySmall,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun WelcomeActions(onRecordFirstNote: () -> Unit, onLookAround: () -> Unit) {
    StepActions {
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
}

@Composable
private fun AnalyticsConsent() {
    Text(
        text = stringResource(R.string.onboarding_analytics_title),
        style = OffhandTheme.type.display,
        color = OffhandTheme.colors.ink,
        modifier = Modifier.padding(top = OffhandSpacing.section).semantics { heading() },
    )
    Text(
        text = stringResource(R.string.onboarding_analytics_body),
        style = OffhandTheme.type.dialogBody,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 12.dp),
    )
    SectionLabel(
        text = stringResource(R.string.onboarding_analytics_shared_label),
        modifier = Modifier.padding(top = OffhandSpacing.section, bottom = 4.dp).semantics { heading() },
    )
    ConsentRows(
        stringResource(R.string.onboarding_analytics_shared_features),
        stringResource(R.string.onboarding_analytics_shared_crashes),
        stringResource(R.string.onboarding_analytics_shared_device),
    )
    SectionLabel(
        text = stringResource(R.string.onboarding_analytics_private_label),
        isEmphasised = true,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp).semantics { heading() },
    )
    ConsentRows(
        stringResource(R.string.onboarding_analytics_private_recordings),
        stringResource(R.string.onboarding_analytics_private_text),
        stringResource(R.string.onboarding_analytics_private_titles),
    )
}

@Composable
private fun AnalyticsConsentActions(onShare: () -> Unit, onDontShare: () -> Unit) {
    StepActions {
        PrimaryButton(
            text = stringResource(R.string.onboarding_analytics_share),
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.onboarding_analytics_dont_share),
            onClick = onDontShare,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Text(
            text = stringResource(R.string.onboarding_analytics_settings_hint),
            style = OffhandTheme.type.caption,
            color = OffhandTheme.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

@Composable
private fun ConsentRows(vararg labels: String) {
    labels.forEachIndexed { index, label ->
        if (index > 0) Hairline()
        Text(
            text = label,
            style = OffhandTheme.type.body,
            color = OffhandTheme.colors.ink,
            modifier = Modifier.fillMaxWidth().heightIn(min = OffhandSpacing.touchTarget).wrapContentHeight(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun StageRow(label: String, meta: String, isMuted: Boolean = false) {
    val color = if (isMuted) OffhandTheme.colors.ink3 else OffhandTheme.colors.ink
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = OffhandTheme.type.body, color = color, modifier = Modifier.weight(1f))
        Text(meta.uppercase(), style = OffhandTheme.type.meta, color = color, maxLines = 1)
    }
}

@Composable
private fun TranscriptsOnly() {
    MetaText(text = stringResource(R.string.onboarding_about_phone), modifier = Modifier.padding(top = 56.dp))
    Text(
        text = stringResource(R.string.onboarding_lite_title),
        style = OffhandTheme.type.display,
        color = OffhandTheme.colors.ink,
        modifier = Modifier.padding(top = 12.dp).semantics { heading() },
    )
    Text(
        text = stringResource(R.string.onboarding_lite_body),
        style = OffhandTheme.type.body,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 16.dp),
    )
    Column(modifier = Modifier.padding(top = 28.dp)) {
        StageRow(stringResource(R.string.onboarding_row_recording), stringResource(R.string.onboarding_included))
        Hairline()
        StageRow(stringResource(R.string.onboarding_row_folders), stringResource(R.string.onboarding_included))
        Hairline()
        StageRow(stringResource(R.string.onboarding_row_summaries), stringResource(R.string.onboarding_not_here), isMuted = true)
    }
    Text(
        text = stringResource(R.string.onboarding_privacy_line),
        style = OffhandTheme.type.bodySmall,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun TranscriptsOnlyActions(onContinue: () -> Unit) {
    StepActions {
        PrimaryButton(
            text = stringResource(R.string.onboarding_lite_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
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
    WaveGlyph(seed = UNSUPPORTED_WAVE_SEED, modifier = Modifier.padding(top = 88.dp))
    Text(
        text = stringResource(R.string.onboarding_incompatible_title),
        style = OffhandTheme.type.display,
        color = OffhandTheme.colors.ink,
        modifier = Modifier.padding(top = 40.dp).semantics { heading() },
    )
    Text(
        text = stringResource(R.string.onboarding_incompatible_body),
        style = OffhandTheme.type.body,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 16.dp),
    )
    specs?.let { SpecRows(it) }
    Text(
        text = stringResource(R.string.onboarding_nothing_stored),
        style = OffhandTheme.type.bodySmall,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun DeviceIncompatibleActions() {
    val activity = LocalActivity.current
    StepActions {
        SecondaryButton(
            text = stringResource(R.string.onboarding_close),
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SpecRows(specs: DeviceSpecsUi) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        SpecRow(
            label = stringResource(R.string.onboarding_this_phone),
            value = if (specs.isCoresSatisfied) {
                stringResource(R.string.onboarding_memory_value, specs.totalRamGb)
            } else {
                stringResource(R.string.onboarding_cores_value, specs.cpuCores)
            },
            isEmphasised = false,
        )
        Hairline()
        SpecRow(
            label = stringResource(R.string.onboarding_needs),
            value = if (specs.isCoresSatisfied) {
                stringResource(R.string.onboarding_needs_value, specs.requiredRamGb)
            } else {
                stringResource(R.string.onboarding_needs_cores_value, specs.requiredCpuCores)
            },
            isEmphasised = true,
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String, isEmphasised: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = OffhandTheme.type.body, color = OffhandTheme.colors.ink, modifier = Modifier.weight(1f))
        Text(
            text = value.uppercase(),
            style = OffhandTheme.type.number,
            color = if (isEmphasised) OffhandTheme.colors.ink else OffhandTheme.colors.ink2,
        )
    }
}

@Preview
@Composable
private fun WelcomePreview() {
    OffhandTheme {
        OnboardingContent(
            state = OnboardingUiState(step = OnboardingStep.WELCOME),
            onRecordFirstNote = {},
            onLookAround = {},
            onContinueWithTranscripts = {},
            onShareAnalytics = {},
            onDontShareAnalytics = {},
        )
    }
}

@Preview
@Composable
private fun AnalyticsConsentPreview() {
    OffhandTheme {
        OnboardingContent(
            state = OnboardingUiState(step = OnboardingStep.ANALYTICS_CONSENT),
            onRecordFirstNote = {},
            onLookAround = {},
            onContinueWithTranscripts = {},
            onShareAnalytics = {},
            onDontShareAnalytics = {},
        )
    }
}

private val MAX_CONTENT_WIDTH = 560.dp
private const val UNSUPPORTED_WAVE_SEED = 3L
