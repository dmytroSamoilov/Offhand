package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.designsystem.R as DesignR
import com.dmytrosamoilov.offhand.core.designsystem.component.MorphingLoadingIndicator
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOptionCard
import com.dmytrosamoilov.offhand.core.ui.component.toDomain
import com.dmytrosamoilov.offhand.core.ui.component.toUi
import com.dmytrosamoilov.offhand.feature.onboarding.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onDownloadContinue() }

    LifecycleResumeEffect(Unit) {
        viewModel.onDeviceLockRecheck()
        onPauseOrDispose { }
    }

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        OnboardingContent(
            state = state,
            onNoteStyleSelected = viewModel::onNoteStyleSelected,
            onDeviceLockSetup = { openSecuritySettings(context) },
            onTelemetryToggled = viewModel::onTelemetryToggled,
            onContinue = { step ->
                when (step) {
                    OnboardingStep.PRIVACY -> viewModel.onPrivacyContinue()
                    OnboardingStep.NOTE_STYLE -> viewModel.onNoteStyleContinue()
                    OnboardingStep.DEVICE_LOCK -> viewModel.onDeviceLockSkipped()
                    OnboardingStep.TELEMETRY_CONSENT -> viewModel.onConsentContinue()
                    OnboardingStep.MODEL_DOWNLOAD ->
                        if (needsNotificationPermission(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onDownloadContinue()
                        }
                    else -> Unit
                }
            },
        )
    }
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    onNoteStyleSelected: (NotePreset) -> Unit,
    onDeviceLockSetup: () -> Unit,
    onTelemetryToggled: (Boolean) -> Unit,
    onContinue: (OnboardingStep) -> Unit,
) {
    when (state.step) {
        OnboardingStep.DEVICE_CHECK -> CenteredPane { MorphingLoadingIndicator() }
        OnboardingStep.DEVICE_INCOMPATIBLE -> CenteredPane {
            DeviceIncompatibleStep(state.deviceSpecs)
        }
        else -> WizardPage(
            state = state,
            onNoteStyleSelected = onNoteStyleSelected,
            onDeviceLockSetup = onDeviceLockSetup,
            onTelemetryToggled = onTelemetryToggled,
            onContinue = onContinue,
        )
    }
}

@Composable
private fun CenteredPane(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun WizardPage(
    state: OnboardingUiState,
    onNoteStyleSelected: (NotePreset) -> Unit,
    onDeviceLockSetup: () -> Unit,
    onTelemetryToggled: (Boolean) -> Unit,
    onContinue: (OnboardingStep) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WizardStepContent(
                    state = state,
                    onNoteStyleSelected = onNoteStyleSelected,
                    onDeviceLockSetup = onDeviceLockSetup,
                    onTelemetryToggled = onTelemetryToggled,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        PageIndicator(currentPage = state.currentPage, pageCount = state.pageCount)
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryStepButton(
            text = stringResource(state.step.continueLabelRes()),
            onClick = { onContinue(state.step) },
        )
    }
}

@Composable
private fun WizardStepContent(
    state: OnboardingUiState,
    onNoteStyleSelected: (NotePreset) -> Unit,
    onDeviceLockSetup: () -> Unit,
    onTelemetryToggled: (Boolean) -> Unit,
) {
    when (state.step) {
        OnboardingStep.PRIVACY -> PrivacyStep()
        OnboardingStep.NOTE_STYLE -> NoteStyleStep(
            selected = state.notePreset.toUi(),
            onSelected = { option -> onNoteStyleSelected(option.toDomain()) },
        )
        OnboardingStep.DEVICE_LOCK -> DeviceLockStep(onSetup = onDeviceLockSetup)
        OnboardingStep.TELEMETRY_CONSENT -> TelemetryConsentStep(
            isTelemetryEnabled = state.isTelemetryEnabled,
            onTelemetryToggled = onTelemetryToggled,
        )
        OnboardingStep.MODEL_DOWNLOAD -> ModelDownloadStep(
            downloadSizeGb = state.downloadSizeGb,
        )
        else -> Unit
    }
}

private fun OnboardingStep.continueLabelRes(): Int =
    if (this == OnboardingStep.DEVICE_LOCK) {
        R.string.onboarding_lock_skip
    } else {
        R.string.onboarding_continue
    }

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            PageDot(isActive = index == currentPage)
        }
    }
}

@Composable
private fun PageDot(isActive: Boolean) {
    val width by animateDpAsState(targetValue = if (isActive) 24.dp else 8.dp)
    val color by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
    )
    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun PrivacyStep() {
    Image(
        painter = painterResource(DesignR.drawable.ic_offhand_logo),
        contentDescription = null,
        modifier = Modifier.size(96.dp),
    )
    Spacer(modifier = Modifier.height(28.dp))
    StepTitle(text = stringResource(R.string.onboarding_privacy_title))
    StepBody(text = stringResource(R.string.onboarding_privacy_body))
}

@Composable
private fun NoteStyleStep(
    selected: NotePresetOption,
    onSelected: (NotePresetOption) -> Unit,
) {
    StepTitle(text = stringResource(R.string.onboarding_note_style_title))
    StepBody(text = stringResource(R.string.onboarding_note_style_body))
    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NotePresetOption.entries.forEach { option ->
            NotePresetOptionCard(
                option = option,
                isSelected = option == selected,
                onClick = { onSelected(option) },
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_note_style_hint),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DeviceLockStep(onSetup: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(28.dp))
    StepTitle(text = stringResource(R.string.onboarding_lock_title))
    StepBody(text = stringResource(R.string.onboarding_lock_body))
    Spacer(modifier = Modifier.height(28.dp))
    Button(onClick = onSetup) {
        Text(text = stringResource(R.string.onboarding_lock_setup))
    }
}

@Composable
private fun TelemetryConsentStep(
    isTelemetryEnabled: Boolean,
    onTelemetryToggled: (Boolean) -> Unit,
) {
    StepTitle(text = stringResource(R.string.onboarding_consent_title))
    StepBody(text = stringResource(R.string.onboarding_consent_body))
    Spacer(modifier = Modifier.height(28.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isTelemetryEnabled,
                role = Role.Switch,
                onValueChange = onTelemetryToggled,
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.onboarding_consent_toggle_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = isTelemetryEnabled, onCheckedChange = null)
        }
    }
}

@Composable
private fun ModelDownloadStep(downloadSizeGb: String) {
    Image(
        painter = painterResource(DesignR.drawable.ic_offhand_logo),
        contentDescription = null,
        modifier = Modifier.size(96.dp),
    )
    Spacer(modifier = Modifier.height(28.dp))
    StepTitle(text = stringResource(R.string.onboarding_download_title))
    StepBody(text = stringResource(R.string.onboarding_download_body))
    Spacer(modifier = Modifier.height(24.dp))
    DownloadSizeBadge(downloadSizeGb = downloadSizeGb)
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_download_background_hint),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_download_wifi_hint),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DeviceIncompatibleStep(specs: DeviceSpecsUi?) {
    StepTitle(text = stringResource(R.string.onboarding_incompatible_title))
    StepBody(text = stringResource(R.string.onboarding_incompatible_body))
    if (specs != null) {
        Spacer(modifier = Modifier.height(24.dp))
        SpecRow(
            label = stringResource(R.string.onboarding_incompatible_ram_label),
            value = stringResource(
                R.string.onboarding_incompatible_ram_value,
                specs.totalRamGb,
                specs.requiredRamGb,
            ),
            isSatisfied = specs.isRamSatisfied,
        )
        SpecRow(
            label = stringResource(R.string.onboarding_incompatible_cores_label),
            value = stringResource(
                R.string.onboarding_incompatible_cores_value,
                specs.cpuCores,
                specs.requiredCpuCores,
            ),
            isSatisfied = specs.isCoresSatisfied,
        )
    }
}

@Composable
private fun PrimaryStepButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun DownloadSizeBadge(downloadSizeGb: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.onboarding_download_size, downloadSizeGb),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun openSecuritySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
    } catch (notFound: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun StepBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SpecRow(label: String, value: String, isSatisfied: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSatisfied) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun OnboardingStatePreview(state: OnboardingUiState) {
    OffhandTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            OnboardingContent(
                state = state,
                onNoteStyleSelected = {},
                onDeviceLockSetup = {},
                onTelemetryToggled = {},
                onContinue = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceCheckPreview() {
    OnboardingStatePreview(OnboardingUiState(step = OnboardingStep.DEVICE_CHECK))
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPreview() {
    OnboardingStatePreview(
        OnboardingUiState(step = OnboardingStep.PRIVACY, currentPage = 0, pageCount = 5),
    )
}

@Preview(showBackground = true)
@Composable
private fun NoteStylePreview() {
    OnboardingStatePreview(
        OnboardingUiState(step = OnboardingStep.NOTE_STYLE, currentPage = 1, pageCount = 5),
    )
}

@Preview(showBackground = true)
@Composable
private fun DeviceLockPreview() {
    OnboardingStatePreview(
        OnboardingUiState(step = OnboardingStep.DEVICE_LOCK, currentPage = 2, pageCount = 5),
    )
}

@Preview(showBackground = true)
@Composable
private fun DeviceIncompatiblePreview() {
    OnboardingStatePreview(
        OnboardingUiState(
            step = OnboardingStep.DEVICE_INCOMPATIBLE,
            deviceSpecs = DeviceSpecsUi(
                totalRamGb = "4.0",
                requiredRamGb = "5.0",
                isRamSatisfied = false,
                cpuCores = 4,
                requiredCpuCores = 4,
                isCoresSatisfied = true,
            ),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun TelemetryConsentPreview() {
    OnboardingStatePreview(
        OnboardingUiState(
            step = OnboardingStep.TELEMETRY_CONSENT,
            currentPage = 3,
            pageCount = 5,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ModelDownloadPreview() {
    OnboardingStatePreview(
        OnboardingUiState(
            step = OnboardingStep.MODEL_DOWNLOAD,
            downloadSizeGb = "2.3",
            currentPage = 4,
            pageCount = 5,
        ),
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ModelDownloadDarkPreview() {
    OnboardingStatePreview(
        OnboardingUiState(
            step = OnboardingStep.MODEL_DOWNLOAD,
            downloadSizeGb = "2.3",
            currentPage = 4,
            pageCount = 5,
        ),
    )
}
