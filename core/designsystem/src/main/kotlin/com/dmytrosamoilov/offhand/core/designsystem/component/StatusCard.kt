package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun StatusCard(
    title: String,
    modifier: Modifier = Modifier,
    number: String? = null,
    progress: Float? = null,
    showProgress: Boolean = false,
    body: String? = null,
    titleTrailing: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    val colors = OffhandTheme.colors
    SurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = title,
                    style = OffhandTheme.type.buttonSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                number?.let { Text(it, style = OffhandTheme.type.number, color = colors.ink2) }
                titleTrailing()
            }
            if (showProgress) ProgressLine(progress = progress, thickness = 3.dp)
            body?.let { Text(it, style = OffhandTheme.type.caption, color = colors.ink2) }
            actions()
        }
    }
}

@Composable
fun MessageCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val colors = OffhandTheme.colors
    SurfaceCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = OffhandTheme.type.body, fontWeight = FontWeight.SemiBold, color = colors.ink)
            Text(body, style = OffhandTheme.type.bodySmall, color = colors.ink2)
            actions()
        }
    }
}
