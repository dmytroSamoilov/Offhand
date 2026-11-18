package com.dmytrosamoilov.offhand.feature.settings.presentation

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.designsystem.component.LargeTitleHeader
import com.dmytrosamoilov.offhand.core.designsystem.component.LinkRow
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.SegmentedControl
import com.dmytrosamoilov.offhand.core.designsystem.component.SwitchRow
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.label
import com.dmytrosamoilov.offhand.feature.recording.domain.FreeImportLimits
import com.dmytrosamoilov.offhand.feature.settings.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onAboutSupportClick: () -> Unit,
    onBackupClick: () -> Unit,
    onNoteStylesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsEffects(state = state, viewModel = viewModel)

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
            LargeTitleHeader(title = stringResource(R.string.settings_title))
            Column(modifier = Modifier.padding(horizontal = OffhandSpacing.card, vertical = OffhandSpacing.card)) {
                PlanCard(status = state.pro, onClick = viewModel::onProCardClicked)
                SubscriptionRows(
                    status = state.pro,
                    onRedeemCodeClick = viewModel::onRedeemCodeClicked,
                    onRedeemCodeConfirm = viewModel::onRedeemCodeConfirmed,
                )
                NotesSection(state = state, viewModel = viewModel, onStylesClick = onNoteStylesClick)
                ImportAndBackupSection(state = state, onBackupClick = onBackupClick, onImportClick = viewModel::onImportAudioClicked)
                PrivacySection(
                    state = state,
                    onAppLockChanged = viewModel::onAppLockChanged,
                    onTelemetryChanged = viewModel::onTelemetryChanged,
                )
                AppSection(state = state, viewModel = viewModel, onAboutClick = onAboutSupportClick)
                state.proOverride?.let { ProOverrideSection(selected = it, onSelected = viewModel::onProOverrideSelected) }
                Spacer(modifier = Modifier.height(OffhandSpacing.section))
            }
        }
    }
}

@Composable
private fun SettingsEffects(state: SettingsUiState, viewModel: SettingsViewModel) {
    val importPicker = LocalAudioImportPicker.current
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.onScreenShown()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.isImportPickerRequested) {
        if (state.isImportPickerRequested) {
            viewModel.onImportPickerOpened()
            importPicker.pick(allowMultiple = state.allowsMultipleImports)
        }
    }
    LaunchedEffect(state.isRedeemFallbackRequested) {
        if (state.isRedeemFallbackRequested) {
            viewModel.onRedeemFallbackOpened()
            openInPlayStore(context, PLAY_REDEEM_URL)
        }
    }
}

@Composable
private fun NotesSection(state: SettingsUiState, viewModel: SettingsViewModel, onStylesClick: () -> Unit) {
    val choices = state.customStyles.map { NoteStyleChoice(id = it.id, name = it.name, description = it.description) }
    val isAvailable = state.isSummaryAvailable
    val unavailable = stringResource(R.string.settings_not_on_this_phone)
    SettingsSection(label = stringResource(R.string.settings_group_notes)) {
        SwitchRow(
            title = stringResource(R.string.settings_transcription_label),
            subtitle = stringResource(R.string.settings_transcription_description),
            checked = state.isTranscriptionEnabled,
            onCheckedChange = viewModel::onTranscriptionChanged,
        )
        SwitchRow(
            title = stringResource(R.string.settings_summary_label),
            subtitle = if (isAvailable) stringResource(R.string.settings_summary_description) else unavailable,
            checked = state.isSummaryEnabled,
            onCheckedChange = viewModel::onSummaryChanged,
            enabled = state.isTranscriptionEnabled && isAvailable,
        )
        LinkRow(
            title = stringResource(R.string.settings_note_styles_title),
            subtitle = if (isAvailable) stringResource(R.string.settings_note_styles_default_value, state.noteStyle.label(choices)) else unavailable,
            onClick = onStylesClick,
            enabled = isAvailable,
        )
        SwitchRow(
            title = stringResource(R.string.settings_smart_suggestions_label),
            subtitle = if (isAvailable) stringResource(R.string.settings_smart_suggestions_description) else unavailable,
            checked = state.isSmartSuggestionsEnabled && isAvailable,
            onCheckedChange = viewModel::onSmartSuggestionsChanged,
            enabled = isAvailable,
            showProBadge = !state.isSmartSuggestionsUnlocked && isAvailable,
        )
        SwitchRow(
            title = stringResource(R.string.settings_skip_silence_label),
            subtitle = stringResource(R.string.settings_skip_silence_description),
            checked = state.isSkipSilenceEnabled,
            onCheckedChange = viewModel::onSkipSilenceChanged,
            showProBadge = !state.isSkipSilenceUnlocked,
            showDivider = false,
        )
    }
    if (!isAvailable) SettingsFootnote(text = stringResource(R.string.settings_summary_unavailable_note))
}

@Composable
private fun ImportAndBackupSection(state: SettingsUiState, onBackupClick: () -> Unit, onImportClick: () -> Unit) {
    var isWidgetHelpVisible by remember { mutableStateOf(false) }
    SettingsSection(label = stringResource(R.string.settings_group_import_backup)) {
        LinkRow(
            title = stringResource(R.string.settings_import_audio_title),
            subtitle = state.freeImportsLeft?.let { stringResource(R.string.settings_import_free_left, it, FreeImportLimits.FREE_IMPORTS) }
                ?: stringResource(R.string.settings_import_audio_subtitle),
            onClick = onImportClick,
            showProBadge = !state.isAudioImportUnlocked,
        )
        LinkRow(
            title = stringResource(R.string.settings_widget_title),
            subtitle = stringResource(R.string.settings_widget_subtitle),
            onClick = { isWidgetHelpVisible = true },
            showProBadge = state.pro == ProStatusUi.Free,
        )
        LinkRow(
            title = stringResource(R.string.settings_backup_title),
            subtitle = stringResource(R.string.settings_backup_subtitle),
            onClick = onBackupClick,
            showDivider = false,
        )
    }
    if (isWidgetHelpVisible) WidgetHelpDialog(onDismiss = { isWidgetHelpVisible = false })
}

@Composable
private fun WidgetHelpDialog(onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.settings_widget_title),
        body = stringResource(R.string.settings_widget_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_import_dismiss),
        onConfirm = onDismiss,
    )
}

@Composable
private fun PrivacySection(
    state: SettingsUiState,
    onAppLockChanged: (Boolean) -> Unit,
    onTelemetryChanged: (Boolean) -> Unit,
) {
    SettingsSection(label = stringResource(R.string.settings_privacy_title)) {
        SwitchRow(
            title = stringResource(R.string.settings_app_lock_label),
            subtitle = stringResource(
                if (state.isDeviceSecure) R.string.settings_app_lock_description else R.string.settings_app_lock_unavailable,
            ),
            checked = state.isAppLockEnabled && state.isDeviceSecure,
            onCheckedChange = onAppLockChanged,
            enabled = state.isDeviceSecure,
        )
        SwitchRow(
            title = stringResource(R.string.settings_telemetry_label),
            subtitle = stringResource(R.string.settings_telemetry_description),
            checked = state.isTelemetryEnabled,
            onCheckedChange = onTelemetryChanged,
            showDivider = false,
        )
    }
    SettingsFootnote(text = stringResource(R.string.settings_about_footer))
}

@Composable
private fun AppSection(state: SettingsUiState, viewModel: SettingsViewModel, onAboutClick: () -> Unit) {
    val context = LocalContext.current
    SettingsSection(label = stringResource(R.string.settings_group_app)) {
        SwitchRow(
            title = stringResource(R.string.settings_dynamic_color_label),
            subtitle = stringResource(R.string.settings_dynamic_color_description),
            checked = state.isDynamicColorEnabled,
            onCheckedChange = viewModel::onDynamicColorChanged,
        )
        LinkRow(
            title = stringResource(R.string.settings_about_support_title),
            subtitle = stringResource(R.string.settings_about_support_subtitle, appVersion(context)),
            onClick = onAboutClick,
            showDivider = false,
        )
    }
}

@Composable
private fun SubscriptionRows(status: ProStatusUi, onRedeemCodeClick: () -> Unit, onRedeemCodeConfirm: () -> Unit) {
    val context = LocalContext.current
    var isHintVisible by remember { mutableStateOf(false) }
    Spacer(modifier = Modifier.height(8.dp))
    SettingsSection(label = null) {
        if (status.isRenewing()) {
            LinkRow(
                title = stringResource(R.string.settings_subscription_manage),
                onClick = { openSubscriptionManagement(context) },
            )
        }
        LinkRow(
            title = stringResource(R.string.settings_redeem_code),
            onClick = {
                onRedeemCodeClick()
                isHintVisible = true
            },
            showDivider = false,
        )
    }
    if (isHintVisible) {
        RedeemCodeDialog(
            onConfirm = {
                isHintVisible = false
                onRedeemCodeConfirm()
            },
            onDismiss = { isHintVisible = false },
        )
    }
}

private fun ProStatusUi.isRenewing(): Boolean =
    this is ProStatusUi.Yearly || this is ProStatusUi.Monthly || this is ProStatusUi.Trial

@Composable
private fun RedeemCodeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.settings_redeem_code),
        body = stringResource(R.string.settings_redeem_code_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_redeem_code_confirm),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.settings_import_pro_cancel),
    )
}

private fun openLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

private fun openInPlayStore(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).setPackage(PLAY_STORE_PACKAGE)
    runCatching { context.startActivity(intent) }.onFailure { openLink(context, url) }
}

private fun openSubscriptionManagement(context: Context) {
    openInPlayStore(context, "https://play.google.com/store/account/subscriptions?sku=offhand_pro&package=${context.packageName}")
}

private const val PLAY_REDEEM_URL = "https://play.google.com/redeem"
private const val PLAY_STORE_PACKAGE = "com.android.vending"

@Composable
internal fun ProStatusUi.statusLabel(): String = when (this) {
    ProStatusUi.Free -> stringResource(R.string.settings_pro_upgrade_subtitle)
    ProStatusUi.Lifetime -> stringResource(R.string.settings_subscription_lifetime)
    ProStatusUi.Included -> stringResource(R.string.settings_subscription_included)
    is ProStatusUi.AppTrial -> stringResource(R.string.settings_plan_trial_body)
    is ProStatusUi.Trial -> endsAtMs?.let { stringResource(R.string.settings_subscription_trial_until, formatDate(it)) }
        ?: stringResource(R.string.settings_subscription_trial)
    is ProStatusUi.Monthly -> renewsAtMs?.let { stringResource(R.string.settings_subscription_monthly_renews, formatDate(it)) }
        ?: stringResource(R.string.settings_subscription_monthly)
    is ProStatusUi.Yearly -> renewsAtMs?.let { stringResource(R.string.settings_subscription_yearly_renews, formatDate(it)) }
        ?: stringResource(R.string.settings_subscription_yearly)
}

@Composable
private fun formatDate(epochMs: Long): String =
    DateUtils.formatDateTime(LocalContext.current, epochMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)

@Composable
private fun ProOverrideSection(selected: ProOverride, onSelected: (ProOverride) -> Unit) {
    SettingsSection(label = stringResource(R.string.settings_developer_title)) {
        Column(modifier = Modifier.padding(OffhandSpacing.card)) {
            Text(
                text = stringResource(R.string.settings_pro_override_label),
                style = OffhandTheme.type.settingTitle,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SegmentedControl(
                options = ProOverride.entries.map { stringResource(it.labelRes()) },
                selectedIndex = ProOverride.entries.indexOf(selected),
                onSelect = { onSelected(ProOverride.entries[it]) },
            )
        }
    }
}

private fun ProOverride.labelRes(): Int = when (this) {
    ProOverride.STORE -> R.string.settings_pro_override_store
    ProOverride.FREE -> R.string.settings_pro_override_free
    ProOverride.PRO -> R.string.settings_pro_override_pro
}

@Composable
internal fun ImportNotice(notice: ImportNoticeUi?, onDismiss: () -> Unit) {
    when (notice) {
        null -> Unit
        is ImportNoticeUi.Started -> ImportNoticeDialog(
            title = stringResource(R.string.settings_import_started_title),
            text = pluralStringResource(R.plurals.settings_import_started, notice.fileCount, notice.fileCount),
            onDismiss = onDismiss,
        )
        ImportNoticeUi.Unreadable -> ImportNoticeDialog(
            title = stringResource(R.string.settings_import_audio_title),
            text = stringResource(R.string.settings_import_unreadable),
            onDismiss = onDismiss,
        )
        ImportNoticeUi.OneVideoAtATime -> ImportNoticeDialog(
            title = stringResource(R.string.settings_import_audio_title),
            text = stringResource(R.string.settings_import_one_video),
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ImportNoticeDialog(title: String, text: String, onDismiss: () -> Unit) {
    OffhandDialog(
        title = title,
        body = text,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_import_dismiss),
        onConfirm = onDismiss,
    )
}
