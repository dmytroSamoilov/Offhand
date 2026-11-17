package com.dmytrosamoilov.offhand.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object OffhandRadius {
    val control: Dp = 12.dp
    val field: Dp = 14.dp
    val card: Dp = 18.dp
    val sheet: Dp = 28.dp
}

object OffhandSpacing {
    val inRow: Dp = 4.dp
    val related: Dp = 8.dp
    val controls: Dp = 12.dp
    val card: Dp = 16.dp
    val screen: Dp = 24.dp
    val section: Dp = 24.dp
    val touchTarget: Dp = 44.dp
    val control: Dp = 56.dp
    val bottomBar: Dp = 104.dp
}

internal val OffhandShapes = Shapes(
    extraSmall = RoundedCornerShape(OffhandRadius.control),
    small = RoundedCornerShape(OffhandRadius.control),
    medium = RoundedCornerShape(OffhandRadius.card),
    large = RoundedCornerShape(OffhandRadius.sheet),
    extraLarge = RoundedCornerShape(OffhandRadius.sheet),
)
