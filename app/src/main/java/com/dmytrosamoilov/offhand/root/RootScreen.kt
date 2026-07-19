package com.dmytrosamoilov.offhand.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.ui.BaseComposeScreen
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingScreen
import com.dmytrosamoilov.offhand.navigation.OffhandApp

@Composable
fun RootScreen(
    requestedNoteId: Long?,
    onRequestedNoteConsumed: () -> Unit,
    viewModel: RootViewModel = hiltViewModel(),
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
    }
}
