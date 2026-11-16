package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class PillAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
fun LabelPill(
    title: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.height(PILL_HEIGHT),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PillIconButton(
    action: PillAction,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = action.onClick,
        modifier = modifier.size(PILL_HEIGHT),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            modifier = Modifier.size(PILL_ICON_SIZE),
        )
    }
}

private val PILL_HEIGHT = 32.dp
private val PILL_ICON_SIZE = 18.dp
