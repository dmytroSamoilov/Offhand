package com.dmytrosamoilov.offhand.root

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.R
import com.dmytrosamoilov.offhand.core.designsystem.component.ProCrown
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingScreen
import com.dmytrosamoilov.offhand.navigation.OffhandApp
import org.koin.androidx.compose.koinViewModel

@Composable
fun RootScreen(
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    viewModel: RootViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BaseComposeScreen(viewModel = viewModel) {
        when (state.phase) {
            RootPhase.LOADING -> Unit
            RootPhase.ONBOARDING -> OnboardingScreen()
            RootPhase.LOCKED -> LockScreen(onAuthenticated = viewModel::onUnlockAuthenticated)
            RootPhase.READY -> OffhandApp(
                requestedNoteId = requestedNoteId,
                onRequestedNoteConsumed = onRequestedNoteConsumed,
            )
        }
        if (state.isEarlyAdopterThanksShown) {
            EarlyAdopterThanksDialog(
                onShowBenefits = viewModel::onEarlyAdopterBenefitsRequested,
                onDismiss = viewModel::onEarlyAdopterThanksDismissed,
            )
        }
    }
}

// The crown and a primary button make the grant hard to tap past; the
// benefits button opens the Pro comparison the paywall shows to subscribers.
@Composable
private fun EarlyAdopterThanksDialog(onShowBenefits: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { ProCrown(size = 48.dp) },
        title = { Text(text = stringResource(R.string.early_adopter_title)) },
        text = { Text(text = stringResource(R.string.early_adopter_body)) },
        confirmButton = {
            Button(onClick = onShowBenefits) { Text(text = stringResource(R.string.early_adopter_benefits)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.early_adopter_confirm)) }
        },
    )
}
