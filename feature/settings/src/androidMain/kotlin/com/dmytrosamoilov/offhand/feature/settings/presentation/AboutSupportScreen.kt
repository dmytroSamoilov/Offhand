package com.dmytrosamoilov.offhand.feature.settings.presentation

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.common.LegalLinks
import com.dmytrosamoilov.offhand.core.designsystem.component.LinkRow
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ProgressLine
import com.dmytrosamoilov.offhand.core.designsystem.component.ScreenTitle
import com.dmytrosamoilov.offhand.core.designsystem.component.SwitchRow
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
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
        SettingsPage(
            topBar = { BackTopBar(backDescription = stringResource(R.string.settings_about_support_back), onBack = onBack) },
        ) {
            ScreenTitle(title = stringResource(R.string.settings_about_support_title), modifier = Modifier.padding(top = 8.dp))
            FeedbackSection()
            if (state.isDeveloperSectionVisible) {
                DeveloperSection(isEnabled = state.isDeveloperOptionsEnabled, onEnabledChanged = viewModel::onDeveloperOptionsChanged)
            }
            if (state.isDeveloperOptionsEnabled && state.isSummaryAvailable) DeveloperTools(state = state, viewModel = viewModel)
            AboutSection()
        }
    }

    if (state.isDeleteModelConfirmationVisible) {
        DeleteModelDialog(onConfirm = viewModel::onDeleteModelConfirmed, onDismiss = viewModel::onDeleteModelDismissed)
    }
}

@Composable
private fun DeveloperTools(state: AboutSupportUiState, viewModel: AboutSupportViewModel) {
    ModelSection(
        state = state,
        onModelSelected = viewModel::onModelSelected,
        onDownload = viewModel::onDownloadModel,
        onDeleteRequested = viewModel::onDeleteModelRequested,
    )
    AccelerationSection(selected = state.selectedBackend, onSelected = viewModel::onBackendSelected)
}

@Composable
private fun DeleteModelDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.settings_delete_model_dialog_title),
        body = stringResource(R.string.settings_delete_model_dialog_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_delete_model_dialog_cancel),
        onConfirm = onDismiss,
        destructiveLabel = stringResource(R.string.settings_delete_model_dialog_confirm),
        onDestructive = onConfirm,
    )
}

@Composable
private fun AccelerationSection(selected: BackendOptionUi, onSelected: (BackendOptionUi) -> Unit) {
    SettingsSection(label = stringResource(R.string.settings_acceleration_title)) {
        SettingsChoiceRow(
            title = stringResource(R.string.settings_acceleration_cpu),
            subtitle = stringResource(R.string.settings_acceleration_cpu_description),
            isSelected = selected == BackendOptionUi.CPU,
            onClick = { onSelected(BackendOptionUi.CPU) },
        )
        SettingsChoiceRow(
            title = stringResource(R.string.settings_acceleration_gpu),
            subtitle = stringResource(R.string.settings_acceleration_gpu_description),
            isSelected = selected == BackendOptionUi.GPU,
            onClick = { onSelected(BackendOptionUi.GPU) },
            showDivider = false,
        )
    }
    SettingsFootnote(text = stringResource(R.string.settings_acceleration_note))
}

@Composable
private fun ModelSection(
    state: AboutSupportUiState,
    onModelSelected: (String?) -> Unit,
    onDownload: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    SettingsSection(label = stringResource(R.string.settings_model_title)) {
        SettingsChoiceRow(
            title = stringResource(R.string.settings_model_picker_auto),
            subtitle = stringResource(R.string.settings_model_picker_auto_description),
            isSelected = state.selectedModelId == null,
            onClick = { onModelSelected(null) },
        )
        state.modelOptions.forEach { option ->
            SettingsChoiceRow(
                title = stringResource(R.string.settings_model_name, option.displayName, option.sizeGb),
                subtitle = option.description,
                isSelected = state.selectedModelId == option.id,
                onClick = { onModelSelected(option.id) },
            )
        }
        ModelStatus(model = state.model, onDownload = onDownload, onDeleteRequested = onDeleteRequested)
    }
    SettingsFootnote(text = stringResource(R.string.settings_model_picker_note))
}

@Composable
private fun ModelStatus(model: ModelUi, onDownload: () -> Unit, onDeleteRequested: () -> Unit) {
    Column(modifier = Modifier.padding(OffhandSpacing.card)) {
        Text(
            text = stringResource(R.string.settings_model_name, model.displayName, model.sizeGb),
            style = OffhandTheme.type.settingTitle,
            color = OffhandTheme.colors.ink,
        )
        if (model.status == ModelStatusUi.DOWNLOADING) {
            ProgressLine(progress = model.downloadPercent / PERCENT, thickness = 3.dp, modifier = Modifier.padding(vertical = 8.dp))
        }
        StatusText(text = model.statusText(), isError = model.status == ModelStatusUi.ERROR)
        ModelAction(status = model.status, onDownload = onDownload, onDeleteRequested = onDeleteRequested)
    }
}

@Composable
private fun ModelUi.statusText(): String = when (status) {
    ModelStatusUi.NOT_DOWNLOADED -> stringResource(R.string.settings_model_status_not_downloaded)
    ModelStatusUi.DOWNLOADING -> stringResource(R.string.settings_model_status_downloading, downloadPercent)
    ModelStatusUi.LOADING -> stringResource(R.string.settings_model_status_loading)
    ModelStatusUi.READY -> stringResource(R.string.settings_model_status_ready)
    ModelStatusUi.ERROR -> errorMessage ?: stringResource(R.string.settings_model_status_error)
}

@Composable
private fun ModelAction(status: ModelStatusUi, onDownload: () -> Unit, onDeleteRequested: () -> Unit) {
    when (status) {
        ModelStatusUi.NOT_DOWNLOADED ->
            PlainTextButton(text = stringResource(R.string.settings_model_download), onClick = onDownload)
        ModelStatusUi.READY -> PlainTextButton(
            text = stringResource(R.string.settings_model_delete),
            onClick = onDeleteRequested,
            tone = TextButtonTone.Danger,
        )
        ModelStatusUi.ERROR -> PlainTextButton(text = stringResource(R.string.settings_model_retry), onClick = onDownload)
        ModelStatusUi.DOWNLOADING, ModelStatusUi.LOADING -> Unit
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


@Composable
private fun FeedbackSection() {
    val context = LocalContext.current
    SettingsSection(label = stringResource(R.string.settings_feedback_title)) {
        LinkRow(
            title = stringResource(R.string.settings_feature_request_label),
            subtitle = stringResource(R.string.settings_feature_request_description),
            onClick = { sendFeatureRequest(context) },
            showDivider = false,
        )
    }
}

@Composable
private fun DeveloperSection(isEnabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    SettingsSection(label = stringResource(R.string.settings_developer_title)) {
        SwitchRow(
            title = stringResource(R.string.settings_developer_label),
            subtitle = stringResource(R.string.settings_developer_description),
            checked = isEnabled,
            onCheckedChange = onEnabledChanged,
            showDivider = false,
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    SettingsSection(label = stringResource(R.string.settings_about_title)) {
        LinkRow(
            title = stringResource(R.string.settings_about_terms),
            onClick = { openLink(context, LegalLinks.TERMS) },
        )
        LinkRow(
            title = stringResource(R.string.settings_about_privacy),
            onClick = { openLink(context, LegalLinks.PRIVACY_POLICY) },
            showDivider = false,
        )
    }
    SettingsFootnote(text = stringResource(R.string.settings_about_version, appVersion(context)))
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

private const val PERCENT = 100f
private const val FEEDBACK_EMAIL = "dmytro@dmytrosamoilov.com"
