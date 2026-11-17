package com.dmytrosamoilov.offhand.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dmytrosamoilov.offhand.core.designsystem.R

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val InterTightFamily = FontFamily(
    Font(R.font.inter_tight_medium, FontWeight.Medium),
    Font(R.font.inter_tight_semibold, FontWeight.SemiBold),
)

val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

@Immutable
data class OffhandTextStyles(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val rowTitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val meta: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val tab: TextStyle,
    val settingTitle: TextStyle,
    val caption: TextStyle,
    val number: TextStyle,
    val timer: TextStyle,
)

private fun inter(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun tight(size: Int, line: Int, tracking: Double) = TextStyle(
    fontFamily = InterTightFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun mono(size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = MonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

internal val QuietInkTextStyles = OffhandTextStyles(
    display = tight(34, 40, -0.02),
    title = tight(28, 34, -0.02),
    heading = tight(22, 28, -0.01),
    rowTitle = inter(17, 22, FontWeight.SemiBold, -0.01),
    body = inter(16, 25, FontWeight.Normal),
    bodySmall = inter(14, 21, FontWeight.Normal),
    label = inter(11, 16, FontWeight.SemiBold, 0.1),
    meta = mono(11, 16, 0.04),
    button = inter(16, 22, FontWeight.SemiBold),
    buttonSmall = inter(15, 22, FontWeight.SemiBold),
    tab = inter(15, 22, FontWeight.Medium),
    settingTitle = inter(15, 21, FontWeight.Medium),
    caption = inter(13, 20, FontWeight.Normal),
    number = mono(13, 18),
    timer = mono(68, 72, -0.04),
)

internal val LocalOffhandTextStyles = staticCompositionLocalOf { QuietInkTextStyles }

internal val OffhandTypography = Typography(
    displayLarge = QuietInkTextStyles.display,
    displayMedium = QuietInkTextStyles.display,
    displaySmall = QuietInkTextStyles.display,
    headlineLarge = QuietInkTextStyles.title,
    headlineMedium = QuietInkTextStyles.title,
    headlineSmall = QuietInkTextStyles.heading,
    titleLarge = QuietInkTextStyles.heading,
    titleMedium = QuietInkTextStyles.rowTitle,
    titleSmall = QuietInkTextStyles.settingTitle,
    bodyLarge = QuietInkTextStyles.body,
    bodyMedium = QuietInkTextStyles.bodySmall,
    bodySmall = QuietInkTextStyles.caption,
    labelLarge = QuietInkTextStyles.buttonSmall,
    labelMedium = inter(12, 16, FontWeight.Medium),
    labelSmall = inter(11, 16, FontWeight.SemiBold),
)
