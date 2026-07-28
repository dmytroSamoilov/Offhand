package com.dmytrosamoilov.offhand.feature.settings.presentation

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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOptionCard
import com.dmytrosamoilov.offhand.feature.settings.R

@Composable
fun SettingsScreen(
    onAboutSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    selected = state.notePreset,
                    onSelected = viewModel::onNotePresetSelected,
                )
                AppearanceSection(
                    isDynamicColorEnabled = state.isDynamicColorEnabled,
                    onDynamicColorChanged = viewModel::onDynamicColorChanged,
                )
                AboutSupportCard(onClick = onAboutSupportClick)
            }
        }
    }
}

@Composable
private fun NoteStyleSection(
    selected: NotePresetOption,
    onSelected: (NotePresetOption) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_note_style_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NotePresetOption.entries.forEach { option ->
                NotePresetOptionCard(
                    option = option,
                    isSelected = option == selected,
                    onClick = { onSelected(option) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_note_style_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun AboutSupportCard(onClick: () -> Unit) {
    val context = LocalContext.current
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_about_support_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.settings_about_support_subtitle,
                        appVersion(context),
                    ),
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
