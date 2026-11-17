package com.dmytrosamoilov.offhand.feature.notes.presentation

// The list row shows a short glyph, the player every measured section; both
// draw bar heights from 0 to 1. An empty list means "not measured", and the
// screens then fall back to their decorative wave.
internal const val LIST_WAVE_BARS = 10

internal fun List<Int>.toWaveformUi(bars: Int = size): List<Float> {
    if (isEmpty() || bars <= 0) return emptyList()
    return List(bars) { bar -> averageOfGroup(bar, bars) / MAX_LEVEL }
}

private fun List<Int>.averageOfGroup(bar: Int, bars: Int): Float {
    val from = bar * size / bars
    val until = ((bar + 1) * size / bars).coerceAtLeast(from + 1)
    return subList(from, until).average().toFloat()
}

private const val MAX_LEVEL = 100f
