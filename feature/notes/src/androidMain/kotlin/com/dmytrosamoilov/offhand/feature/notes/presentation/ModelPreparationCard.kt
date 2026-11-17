package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.StatusCard
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

@Composable
internal fun ModelPreparationCard(
    preparation: ModelPreparationUi?,
    onDownloadOnMobileData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastVisible by remember { mutableStateOf<ModelPreparationUi>(ModelPreparationUi.Downloading(progressPercent = 0)) }
    if (preparation != null) lastVisible = preparation
    AnimatedVisibility(
        visible = preparation != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        when (val visible = lastVisible) {
            is ModelPreparationUi.Downloading -> DownloadingCard(progressPercent = visible.progressPercent)
            is ModelPreparationUi.WaitingForMobileData -> WaitingForMobileDataCard(
                sizeGb = visible.sizeGb,
                onDownloadOnMobileData = onDownloadOnMobileData,
            )
        }
    }
}

@Composable
private fun DownloadingCard(progressPercent: Int) {
    val progress by animateFloatAsState(targetValue = progressPercent / PERCENT, animationSpec = OffhandMotion.state(), label = "model-progress")
    StatusCard(
        title = stringResource(R.string.notes_model_banner_title),
        number = stringResource(R.string.notes_model_banner_percent, progressPercent),
        progress = progress,
        showProgress = true,
        body = stringResource(R.string.notes_model_banner_subtitle),
        titleTrailing = { ModelInfoButton() },
    )
}

@Composable
private fun WaitingForMobileDataCard(sizeGb: String, onDownloadOnMobileData: () -> Unit) {
    StatusCard(
        title = stringResource(R.string.notes_model_banner_waiting_title),
        body = stringResource(R.string.notes_model_banner_waiting_subtitle, sizeGb),
        titleTrailing = { ModelInfoButton() },
        actions = {
            PlainTextButton(
                text = stringResource(R.string.notes_model_banner_mobile_data_button),
                onClick = onDownloadOnMobileData,
                modifier = Modifier.offset(x = (-12).dp),
            )
        },
    )
}

@Composable
private fun ModelInfoButton() {
    var isInfoVisible by remember { mutableStateOf(false) }
    BarIconButton(
        icon = OffhandIcons.Info,
        contentDescription = stringResource(R.string.notes_model_banner_info_description),
        onClick = { isInfoVisible = true },
        tint = OffhandTheme.colors.ink2,
    )
    if (isInfoVisible) {
        NoticeDialog(
            titleRes = R.string.notes_model_banner_info_title,
            bodyRes = R.string.notes_model_banner_info_body,
            dismissRes = R.string.notes_model_banner_info_dismiss,
            onDismiss = { isInfoVisible = false },
        )
    }
}

private const val PERCENT = 100f
