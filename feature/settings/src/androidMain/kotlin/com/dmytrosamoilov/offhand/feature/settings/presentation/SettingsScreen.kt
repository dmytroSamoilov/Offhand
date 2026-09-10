package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.CustomNoteStyleIcon
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOptionCard
import com.dmytrosamoilov.offhand.core.ui.component.NoteStyleCard
import com.dmytrosamoilov.offhand.core.ui.component.toDomain
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
                NoteStyleSection(
                    selected = state.noteStyle,
                    customStyles = state.customStyles,
                    onSelected = viewModel::onNoteStyleSelected,
                    onManageClick = onNoteStylesClick,
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
                BackupCard(onClick = onBackupClick)
                if (state.isAudioImportUnlocked) {
                    ImportAudioCard(onClick = { importLauncher.launch(arrayOf(AUDIO_MIME_TYPE)) })
                }
                AboutSupportCard(onClick = onAboutSupportClick)
            }
        }
    }
}

@Composable
private fun NoteStyleSection(
    selected: NoteStyleRef,
    customStyles: List<CustomStyleOptionUi>,
    onSelected: (NoteStyleRef) -> Unit,
    onManageClick: () -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_note_style_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NotePresetOption.entries.forEach { option ->
                val style = NoteStyleRef.BuiltIn(option.toDomain())
                NotePresetOptionCard(
                    option = option,
                    isSelected = style == selected,
                    onClick = { onSelected(style) },
                )
            }
            customStyles.forEach { custom ->
                val style = NoteStyleRef.Custom(custom.id)
                NoteStyleCard(
                    title = custom.name,
                    description = custom.description,
                    icon = CustomNoteStyleIcon,
                    isSelected = style == selected,
                    onClick = { onSelected(style) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ManageStylesRow(onClick = onManageClick)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_note_style_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManageStylesRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_note_styles_manage),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.settings_note_styles_manage_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
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
private fun ImportAudioCard(onClick: () -> Unit) {
    NavigationCard(
        title = stringResource(R.string.settings_import_audio_title),
        subtitle = stringResource(R.string.settings_import_audio_subtitle),
        onClick = onClick,
    )
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
        ImportNoticeUi.Locked -> ImportNoticeDialog(
            title = stringResource(R.string.settings_import_audio_title),
            text = stringResource(R.string.settings_import_locked),
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
private fun BackupCard(onClick: () -> Unit) {
    NavigationCard(
        title = stringResource(R.string.settings_backup_title),
        subtitle = stringResource(R.string.settings_backup_subtitle),
        onClick = onClick,
    )
}

@Composable
private fun NavigationCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutSupportCard(onClick: () -> Unit) {
    val context = LocalContext.current
    NavigationCard(
        title = stringResource(R.string.settings_about_support_title),
        subtitle = stringResource(R.string.settings_about_support_subtitle, appVersion(context)),
        onClick = onClick,
    )
}
