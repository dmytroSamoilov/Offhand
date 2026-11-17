package com.dmytrosamoilov.offhand.root

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.R
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingScreen
import com.dmytrosamoilov.offhand.navigation.OffhandApp
import org.koin.androidx.compose.koinViewModel

@Composable
fun RootScreen(
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    requestedRecording: RecordingRequestUi?,
    onRequestedRecordingConsumed: () -> Unit,
    viewModel: RootViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(requestedRecording) {
        if (requestedRecording != null) {
            viewModel.onRecordingRequested(requestedRecording.folderId)
            onRequestedRecordingConsumed()
        }
    }

    BaseComposeScreen(viewModel = viewModel) {
        when (state.phase) {
            RootPhase.LOADING -> Unit
            RootPhase.ONBOARDING -> OnboardingScreen()
            RootPhase.LOCKED -> LockScreen(onAuthenticated = viewModel::onUnlockAuthenticated)
            RootPhase.READY -> OffhandApp(
                requestedNoteId = requestedNoteId,
                onRequestedNoteConsumed = onRequestedNoteConsumed,
                recordingRequest = state.recordingRequest,
                onRecordingRequestConsumed = viewModel::onRecordingRequestConsumed,
            )
        }
        if (state.isEarlyAdopterThanksShown) {
            EarlyAdopterThanksDialog(
                onShowBenefits = viewModel::onEarlyAdopterBenefitsRequested,
                onDismiss = viewModel::onEarlyAdopterThanksDismissed,
            )
        }
        if (state.trialNotice == ProTrialNotice.REMOVED) {
            TrialRemovedDialog(
                onUpgrade = viewModel::onTrialUpgradeRequested,
                onDismiss = viewModel::onTrialNoticeDismissed,
            )
        }
    }
}

@Composable
private fun EarlyAdopterThanksDialog(onShowBenefits: () -> Unit, onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.early_adopter_title),
        body = stringResource(R.string.early_adopter_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.early_adopter_benefits),
        onConfirm = onShowBenefits,
        dismissLabel = stringResource(R.string.early_adopter_confirm),
    ) {
        ProCrown(size = 40.dp, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun TrialRemovedDialog(onUpgrade: () -> Unit, onDismiss: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.trial_removed_title),
        body = stringResource(R.string.trial_removed_body),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.trial_notice_upgrade),
        onConfirm = onUpgrade,
        dismissLabel = stringResource(R.string.trial_notice_keep_free),
    )
}
