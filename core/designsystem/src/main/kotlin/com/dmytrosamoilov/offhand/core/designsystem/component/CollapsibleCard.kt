package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.R

@Composable
fun CollapsibleCard(
    title: String,
    modifier: Modifier = Modifier,
    labelContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    collapsedMaxHeight: Dp = DEFAULT_COLLAPSED_MAX_HEIGHT,
    initiallyExpanded: Boolean = false,
    action: CollapsibleCardAction? = null,
    content: @Composable () -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var isOverflowing by remember { mutableStateOf(false) }
    val collapsedMaxHeightPx = with(LocalDensity.current) { collapsedMaxHeight.roundToPx() }
    val containerColor = CardDefaults.cardColors().containerColor

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .animateContentSize(animationSpec = tween(durationMillis = EXPAND_ANIMATION_MS)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelPill(
                    title = title,
                    containerColor = labelContainerColor,
                    contentColor = labelContentColor,
                )
                if (isOverflowing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PillIconButton(
                        icon = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (isExpanded) R.string.designsystem_show_less else R.string.designsystem_show_more,
                        ),
                        containerColor = labelContainerColor,
                        contentColor = labelContentColor,
                        onClick = { isExpanded = !isExpanded },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (action != null) {
                    PillIconButton(
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                        containerColor = labelContainerColor,
                        contentColor = labelContentColor,
                        onClick = action.onClick,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = if (isExpanded) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = collapsedMaxHeight)
                        .clipToBounds()
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .onSizeChanged { size ->
                            isOverflowing = size.height > collapsedMaxHeightPx
                        },
                ) {
                    content()
                }
                if (!isExpanded && isOverflowing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(FADE_HEIGHT)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, containerColor),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

data class CollapsibleCardAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
private fun LabelPill(title: String, containerColor: Color, contentColor: Color) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.height(PILL_HEIGHT),
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
private fun PillIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(PILL_HEIGHT),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(PILL_ICON_SIZE),
        )
    }
}

private val PILL_HEIGHT = 32.dp
private val PILL_ICON_SIZE = 18.dp
private val DEFAULT_COLLAPSED_MAX_HEIGHT = 168.dp
private val FADE_HEIGHT = 56.dp
private const val EXPAND_ANIMATION_MS = 300
