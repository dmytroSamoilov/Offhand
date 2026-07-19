package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.R as DesignR
import com.dmytrosamoilov.offhand.core.designsystem.component.MorphingLoadingIndicator
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.onboarding.R

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onDownloadContinue() }

    BaseComposeScreen(viewModel = viewModel, modifier = modifier) {
        OnboardingContent(
            state = state,
            onDownloadContinue = {
                if (needsNotificationPermission(context)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.onDownloadContinue()
                }
            },
            onConsentChosen = viewModel::onConsentChosen,
        )
    }
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    onDownloadContinue: () -> Unit,
    onConsentChosen: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.step) {
            OnboardingStep.DEVICE_CHECK -> MorphingLoadingIndicator()
            OnboardingStep.DEVICE_INCOMPATIBLE -> DeviceIncompatibleStep(state.deviceSpecs)
            OnboardingStep.MODEL_DOWNLOAD -> ModelDownloadStep(
                downloadSizeGb = state.downloadSizeGb,
                onContinue = onDownloadContinue,
            )
            OnboardingStep.TELEMETRY_CONSENT -> TelemetryConsentStep(
                onConsentChosen = onConsentChosen,
            )
        }
    }
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
private fun ModelDownloadStep(
    downloadSizeGb: String,
    onContinue: () -> Unit,
) {
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
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onContinue,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
    ) {
        Text(
            text = stringResource(R.string.onboarding_download_continue),
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

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED

@Composable
private fun TelemetryConsentStep(onConsentChosen: (Boolean) -> Unit) {
    StepTitle(text = stringResource(R.string.onboarding_consent_title))
    StepBody(text = stringResource(R.string.onboarding_consent_body))
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { onConsentChosen(true) }) {
        Text(text = stringResource(R.string.onboarding_consent_allow))
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = { onConsentChosen(false) }) {
        Text(text = stringResource(R.string.onboarding_consent_decline))
    }
}

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
                onDownloadContinue = {},
                onConsentChosen = {},
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
private fun ModelDownloadPreview() {
    OnboardingStatePreview(
        OnboardingUiState(
            step = OnboardingStep.MODEL_DOWNLOAD,
            downloadSizeGb = "2.3",
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
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun TelemetryConsentPreview() {
    OnboardingStatePreview(OnboardingUiState(step = OnboardingStep.TELEMETRY_CONSENT))
}
