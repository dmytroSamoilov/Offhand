package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleChoice
import com.dmytrosamoilov.offhand.core.ui.component.NoteStylePickerSheet
import com.dmytrosamoilov.offhand.core.ui.component.label
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportIntake
import com.dmytrosamoilov.offhand.feature.settings.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onAboutSupportClick: () -> Unit,
    onBackupClick: () -> Unit,
    onNoteStylesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importIntake: AudioImportIntake = koinInject()
    val importScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importScope.launch {
            val staged = uris.map { uri -> importIntake.stage(uri) }
            viewModel.onAudioImportSelected(staged.filterNotNull(), staged.count { it == null })
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onScreenShown()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.isImportPickerRequested) {
        if (state.isImportPickerRequested) {
            viewModel.onImportPickerOpened()
            importLauncher.launch(arrayOf(AUDIO_MIME_TYPE))
        }
    }
    ImportNotice(notice = state.importNotice, onDismiss = viewModel::onImportNoticeDismissed)

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Scaffold(
            topBar = { AppTopBar(title = stringResource(R.string.settings_title)) },
            contentWindowInsets = WindowInsets(0.dp),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProSection(
                    status = state.pro,
                    onUpgradeClick = viewModel::onUpgradeClicked,
                )
                NotesSection(
                    state = state,
                    onStyleSelected = viewModel::onNoteStyleSelected,
                    onManageClick = onNoteStylesClick,
                    onSmartSuggestionsChanged = viewModel::onSmartSuggestionsChanged,
                )
                SecuritySection(
                    isAppLockEnabled = state.isAppLockEnabled,
                    isDeviceSecure = state.isDeviceSecure,
                    onAppLockChanged = viewModel::onAppLockChanged,
                )
                AppearanceSection(
                    isDynamicColorEnabled = state.isDynamicColorEnabled,
                    onDynamicColorChanged = viewModel::onDynamicColorChanged,
                )
                BackupSection(
                    isImportUnlocked = state.isAudioImportUnlocked,
                    onBackupClick = onBackupClick,
                    onImportClick = viewModel::onImportAudioClicked,
                )
                AboutSection(onClick = onAboutSupportClick)
                state.proOverride?.let { override ->
                    ProOverrideSection(selected = override, onSelected = viewModel::onProOverrideSelected)
                }
            }
        }
    }
}

// Mirrors the iOS form: one Notes group with the default style, the style
// manager and the suggestions switch.
@Composable
private fun NotesSection(
    state: SettingsUiState,
    onStyleSelected: (NoteStyleRef) -> Unit,
    onManageClick: () -> Unit,
    onSmartSuggestionsChanged: (Boolean) -> Unit,
) {
    var isPickerVisible by remember { mutableStateOf(false) }
    val choices = state.customStyles.map { NoteStyleChoice(id = it.id, name = it.name, description = it.description) }
    SettingsCard(title = stringResource(R.string.settings_notes_title)) {
        DefaultStyleRow(label = state.noteStyle.label(choices), onClick = { isPickerVisible = true })
        SettingsLinkRow(
            title = stringResource(R.string.settings_note_styles_manage),
            subtitle = stringResource(R.string.settings_note_styles_manage_subtitle),
            onClick = onManageClick,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SwitchRow(
            label = stringResource(R.string.settings_smart_suggestions_label),
            description = stringResource(R.string.settings_smart_suggestions_description),
            checked = state.isSmartSuggestionsEnabled,
            onCheckedChange = onSmartSuggestionsChanged,
            showProBadge = !state.isSmartSuggestionsUnlocked,
        )
    }
    if (isPickerVisible) {
        NoteStylePickerSheet(
            title = stringResource(R.string.settings_note_style_title),
            body = stringResource(R.string.settings_note_style_note),
            selected = state.noteStyle,
            customStyles = choices,
            isCustomStylesUnlocked = state.isCustomStylesUnlocked,
            onSelected = { style ->
                isPickerVisible = false
                onStyleSelected(style)
            },
            onDismiss = { isPickerVisible = false },
        )
    }
}

@Composable
private fun DefaultStyleRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_note_style_title),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SecuritySection(
    isAppLockEnabled: Boolean,
    isDeviceSecure: Boolean,
    onAppLockChanged: (Boolean) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_security_title)) {
        SwitchRow(
            label = stringResource(R.string.settings_app_lock_label),
            description = if (isDeviceSecure) {
                stringResource(R.string.settings_app_lock_description)
            } else {
                stringResource(R.string.settings_app_lock_unavailable)
            },
            checked = isAppLockEnabled && isDeviceSecure,
            onCheckedChange = onAppLockChanged,
            enabled = isDeviceSecure,
        )
    }
}

@Composable
private fun AppearanceSection(
    isDynamicColorEnabled: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_appearance_title)) {
        SwitchRow(
            label = stringResource(R.string.settings_dynamic_color_label),
            description = stringResource(R.string.settings_dynamic_color_description),
            checked = isDynamicColorEnabled,
            onCheckedChange = onDynamicColorChanged,
        )
    }
}

@Composable
private fun ProSection(status: ProStatusUi, onUpgradeClick: () -> Unit) {
    when (status) {
        ProStatusUi.Free -> UpgradeCard(onClick = onUpgradeClick)
        else -> SubscriptionSection(status = status)
    }
}

@Composable
private fun UpgradeCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProCrown(size = 28.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_pro_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_pro_upgrade_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Button(onClick = onClick) { Text(text = stringResource(R.string.settings_pro_upgrade)) }
        }
    }
}

@Composable
private fun SubscriptionSection(status: ProStatusUi) {
    val context = LocalContext.current
    SettingsCard(title = stringResource(R.string.settings_subscription_title)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProCrown(size = 24.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_pro_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = status.statusLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (status != ProStatusUi.Lifetime) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { openSubscriptionManagement(context) }) {
                Text(text = stringResource(R.string.settings_subscription_manage))
            }
        }
    }
}

@Composable
private fun ProStatusUi.statusLabel(): String = when (this) {
    ProStatusUi.Free -> ""
    ProStatusUi.Lifetime -> stringResource(R.string.settings_subscription_lifetime)
    is ProStatusUi.Trial -> endsAtMs?.let { stringResource(R.string.settings_subscription_trial_until, formatDate(it)) }
        ?: stringResource(R.string.settings_subscription_trial)
    is ProStatusUi.Yearly -> renewsAtMs?.let { stringResource(R.string.settings_subscription_yearly_renews, formatDate(it)) }
        ?: stringResource(R.string.settings_subscription_yearly)
}

@Composable
private fun formatDate(epochMs: Long): String =
    DateUtils.formatDateTime(LocalContext.current, epochMs, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)

// Play's subscription center for this product; Play requires an in-app way
// to reach it and it is where cancellation lives.
private fun openSubscriptionManagement(context: Context) {
    val url = "https://play.google.com/store/account/subscriptions?sku=offhand_pro&package=${context.packageName}"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

@Composable
private fun ProOverrideSection(selected: ProOverride, onSelected: (ProOverride) -> Unit) {
    SettingsCard(title = stringResource(R.string.settings_developer_title)) {
        Text(
            text = stringResource(R.string.settings_pro_override_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ProOverride.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ProOverride.entries.size),
                ) {
                    Text(text = stringResource(option.labelRes()))
                }
            }
        }
    }
}

private fun ProOverride.labelRes(): Int = when (this) {
    ProOverride.STORE -> R.string.settings_pro_override_store
    ProOverride.FREE -> R.string.settings_pro_override_free
    ProOverride.PRO -> R.string.settings_pro_override_pro
}

@Composable
private fun ImportNotice(notice: ImportNoticeUi?, onDismiss: () -> Unit) {
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
    }
}

@Composable
private fun ImportNoticeDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.settings_import_dismiss)) }
        },
    )
}

private const val AUDIO_MIME_TYPE = "audio/*"

@Composable
private fun BackupSection(isImportUnlocked: Boolean, onBackupClick: () -> Unit, onImportClick: () -> Unit) {
    SettingsCard(title = stringResource(R.string.settings_backup_section_title)) {
        SettingsLinkRow(
            title = stringResource(R.string.settings_backup_title),
            subtitle = stringResource(R.string.settings_backup_subtitle),
            onClick = onBackupClick,
        )
        SettingsLinkRow(
            title = stringResource(R.string.settings_import_audio_title),
            subtitle = stringResource(R.string.settings_import_audio_subtitle),
            onClick = onImportClick,
            showProBadge = !isImportUnlocked,
        )
    }
}

@Composable
private fun AboutSection(onClick: () -> Unit) {
    val context = LocalContext.current
    SettingsCard(title = stringResource(R.string.settings_about_section_title)) {
        SettingsLinkRow(
            title = stringResource(R.string.settings_about_support_title),
            subtitle = stringResource(R.string.settings_about_support_subtitle, appVersion(context)),
            onClick = onClick,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_about_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
