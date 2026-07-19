package com.dmytrosamoilov.offhand.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Baseline = Typography()

internal val OffhandTypography = Baseline.copy(
    displaySmall = Baseline.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Baseline.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Baseline.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Baseline.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Baseline.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Baseline.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
