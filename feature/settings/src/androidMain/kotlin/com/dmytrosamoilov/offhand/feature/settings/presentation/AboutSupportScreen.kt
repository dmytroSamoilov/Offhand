package com.dmytrosamoilov.offhand.feature.settings.presentation

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.AppTopBar
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.settings.R
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@Composable
fun AboutSupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutSupportViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        Scaffold(
            topBar = { AboutSupportTopBar(onBack = onBack) },
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
                FeedbackSection()
                PrivacySection(
                    isTelemetryEnabled = state.isTelemetryEnabled,
                    onTelemetryChanged = viewModel::onTelemetryChanged,
                )
                if (state.isDeveloperSectionVisible) {
                    DeveloperSection(
                        isEnabled = state.isDeveloperOptionsEnabled,
                        onEnabledChanged = viewModel::onDeveloperOptionsChanged,
                    )
                }
                if (state.isDeveloperOptionsEnabled) {
                    ModelSection(
                        model = state.model,
                        modelOptions = state.modelOptions,
                        selectedModelId = state.selectedModelId,
                        onModelSelected = viewModel::onModelSelected,
                        onDownload = viewModel::onDownloadModel,
                        onDeleteRequested = viewModel::onDeleteModelRequested,
                    )
                    AccelerationSection(
                        selected = state.selectedBackend,
                        onSelected = viewModel::onBackendSelected,
                    )
                }
                AboutSection()
            }
        }
    }

    if (state.isDeleteModelConfirmationVisible) {
        DeleteModelDialog(
            onConfirm = viewModel::onDeleteModelConfirmed,
            onDismiss = viewModel::onDeleteModelDismissed,
        )
    }
}

@Composable
private fun AboutSupportTopBar(onBack: () -> Unit) {
    AppTopBar(
        title = stringResource(R.string.settings_about_support_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_about_support_back),
                )
            }
        },
    )
}

@Composable
private fun DeleteModelDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_delete_model_dialog_title)) },
        text = { Text(text = stringResource(R.string.settings_delete_model_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.settings_delete_model_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_delete_model_dialog_cancel))
            }
        },
    )
}

@Composable
private fun AccelerationSection(
    selected: BackendOptionUi,
    onSelected: (BackendOptionUi) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_acceleration_title)) {
        RadioOptionRow(
            label = stringResource(R.string.settings_acceleration_cpu),
            description = stringResource(R.string.settings_acceleration_cpu_description),
            isSelected = selected == BackendOptionUi.CPU,
            onClick = { onSelected(BackendOptionUi.CPU) },
        )
        RadioOptionRow(
            label = stringResource(R.string.settings_acceleration_gpu),
            description = stringResource(R.string.settings_acceleration_gpu_description),
            isSelected = selected == BackendOptionUi.GPU,
            onClick = { onSelected(BackendOptionUi.GPU) },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_acceleration_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelSection(
    model: ModelUi,
    modelOptions: List<ModelOptionUi>,
    selectedModelId: String?,
    onModelSelected: (String?) -> Unit,
    onDownload: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_model_title)) {
        RadioOptionRow(
            label = stringResource(R.string.settings_model_picker_auto),
            description = stringResource(R.string.settings_model_picker_auto_description),
            isSelected = selectedModelId == null,
            onClick = { onModelSelected(null) },
        )
        modelOptions.forEach { option ->
            RadioOptionRow(
                label = stringResource(
                    R.string.settings_model_name,
                    option.displayName,
                    option.sizeGb,
                ),
                description = option.description,
                isSelected = selectedModelId == option.id,
                onClick = { onModelSelected(option.id) },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_model_picker_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_model_name, model.displayName, model.sizeGb),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ModelStatus(model = model, onDownload = onDownload, onDeleteRequested = onDeleteRequested)
    }
}

@Composable
private fun ModelStatus(
    model: ModelUi,
    onDownload: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    when (model.status) {
        ModelStatusUi.NOT_DOWNLOADED -> {
            StatusText(text = stringResource(R.string.settings_model_status_not_downloaded))
            TextButton(onClick = onDownload) {
                Text(text = stringResource(R.string.settings_model_download))
            }
        }
        ModelStatusUi.DOWNLOADING -> {
            LinearProgressIndicator(
                progress = { model.downloadPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusText(
                text = stringResource(
                    R.string.settings_model_status_downloading,
                    model.downloadPercent,
                ),
            )
        }
        ModelStatusUi.LOADING -> StatusText(
            text = stringResource(R.string.settings_model_status_loading),
        )
        ModelStatusUi.READY -> {
            StatusText(text = stringResource(R.string.settings_model_status_ready))
            TextButton(onClick = onDeleteRequested) {
                Text(
                    text = stringResource(R.string.settings_model_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        ModelStatusUi.ERROR -> {
            StatusText(
                text = model.errorMessage
                    ?: stringResource(R.string.settings_model_status_error),
                isError = true,
            )
            TextButton(onClick = onDownload) {
                Text(text = stringResource(R.string.settings_model_retry))
            }
        }
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun PrivacySection(
    isTelemetryEnabled: Boolean,
    onTelemetryChanged: (Boolean) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_privacy_title)) {
        SwitchRow(
            label = stringResource(R.string.settings_telemetry_label),
            description = stringResource(R.string.settings_telemetry_description),
            checked = isTelemetryEnabled,
            onCheckedChange = onTelemetryChanged,
        )
    }
}

@Composable
private fun FeedbackSection() {
    val context = LocalContext.current
    SettingsCard(title = stringResource(R.string.settings_feedback_title)) {
        Text(
            text = stringResource(R.string.settings_feature_request_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { sendFeatureRequest(context) }) {
            Text(text = stringResource(R.string.settings_feature_request_label))
        }
    }
}

@Composable
private fun DeveloperSection(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    SettingsCard(title = stringResource(R.string.settings_developer_title)) {
        SwitchRow(
            label = stringResource(R.string.settings_developer_label),
            description = stringResource(R.string.settings_developer_description),
            checked = isEnabled,
            onCheckedChange = onEnabledChanged,
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    SettingsCard(title = stringResource(R.string.settings_about_title)) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.settings_about_version, appVersion(context)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = { openLink(context, TERMS_URL) }) {
            Text(text = stringResource(R.string.settings_about_terms))
        }
        TextButton(onClick = { openLink(context, PRIVACY_POLICY_URL) }) {
            Text(text = stringResource(R.string.settings_about_privacy))
        }
    }
}

private fun sendFeatureRequest(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO)
        .setData(Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, featureRequestSubject(context))
        .putExtra(Intent.EXTRA_TEXT, featureRequestBody(context))
    try {
        context.startActivity(intent)
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(
            context,
            R.string.settings_feature_request_no_email_app,
            Toast.LENGTH_SHORT,
        ).show()
    }
}

private fun featureRequestSubject(context: Context): String {
    val appLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
    return context.getString(R.string.settings_feature_request_subject, appLabel)
}

private fun featureRequestBody(context: Context): String = context.getString(
    R.string.settings_feature_request_body,
    Build.BRAND,
    Build.MODEL,
    Build.DEVICE,
    totalRam(context),
    appVersion(context),
    Build.VERSION.RELEASE,
    Build.VERSION.SDK_INT,
)

private fun totalRam(context: Context): String {
    val activityManager = context.getSystemService(ActivityManager::class.java) ?: return "unknown"
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val gigabytes = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f GB", gigabytes)
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(context, R.string.settings_about_no_browser, Toast.LENGTH_SHORT).show()
    }
}

internal fun appVersion(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    "${info.versionName}"
}.getOrDefault("unknown")

private const val FEEDBACK_EMAIL = "dmytro@dmytrosamoilov.com"
private const val TERMS_URL = "https://dmytrosamoilov.com/offhand/terms-and-conditions"
private const val PRIVACY_POLICY_URL = "https://dmytrosamoilov.com/offhand/privacy-policy"
