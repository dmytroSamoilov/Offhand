package com.dmytrosamoilov.offhand.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun BaseComposeScreen(
    viewModel: BaseViewModel,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OffhandTheme.colors.ground),
    ) {
        content()
        if (isLoading) {
            LoadingOverlay()
        }
    }

    errorMessage?.let { message ->
        ErrorDialog(message = message, onDismiss = viewModel::dismissError)
    }
}

@Composable
private fun BoxScope.LoadingOverlay() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(OffhandTheme.colors.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(OffhandRadius.card))
                .background(OffhandTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = OffhandTheme.colors.ink,
                trackColor = OffhandTheme.colors.hairline,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    OffhandDialog(
        title = stringResource(R.string.core_ui_error_title),
        body = message,
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.core_ui_error_confirm),
        onConfirm = onDismiss,
    )
}
