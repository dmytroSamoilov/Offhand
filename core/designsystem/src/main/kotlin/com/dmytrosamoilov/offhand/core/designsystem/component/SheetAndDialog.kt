package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffhandSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    titleTrailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OffhandTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = OffhandRadius.sheet, topEnd = OffhandRadius.sheet),
        containerColor = colors.surface,
        contentColor = colors.ink,
        scrimColor = colors.scrim,
        tonalElevation = 0.dp,
        dragHandle = { Grabber() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
            title?.let {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SheetHeading(text = it, modifier = Modifier.weight(1f))
                    titleTrailing()
                }
            }
            content()
        }
    }
}

@Composable
fun SheetHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = OffhandTheme.type.heading,
        color = OffhandTheme.colors.ink,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun Grabber() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 10.dp)
            .size(36.dp, 4.dp)
            .clip(CircleShape)
            .background(OffhandTheme.colors.border),
    )
}

@Composable
fun OffhandDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    destructiveLabel: String? = null,
    onDestructive: () -> Unit = {},
    dismissLabel: String? = null,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = OffhandTheme.colors
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(OffhandRadius.sheet))
                .background(colors.surface)
                .border(1.dp, if (colors.isDark) colors.border else colors.surface, RoundedCornerShape(OffhandRadius.sheet))
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            SheetHeading(text = title)
            body?.let {
                Text(
                    text = it,
                    style = OffhandTheme.type.dialogBody,
                    color = colors.ink2,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            content()
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                confirmLabel?.let {
                    PrimaryButton(text = it, onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.fillMaxWidth())
                }
                destructiveLabel?.let {
                    PlainTextButton(text = it, onClick = onDestructive, tone = TextButtonTone.Danger, modifier = Modifier.fillMaxWidth())
                }
                dismissLabel?.let {
                    PlainTextButton(text = it, onClick = onDismissRequest, tone = TextButtonTone.Muted, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
