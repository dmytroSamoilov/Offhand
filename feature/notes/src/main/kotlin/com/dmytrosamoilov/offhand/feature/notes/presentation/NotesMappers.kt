package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.audio.PcmPlaybackState
import com.dmytrosamoilov.offhand.core.common.DurationFormatter
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.US)
private val DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val MARKDOWN_TOKENS = Regex("[#*>`_\\[\\]]")
private val WHITESPACE_RUNS = Regex("\\s+")
private const val PREVIEW_MAX_CHARS = 220

internal fun List<Note>.toSectionsUi(): List<NotesSectionUi> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    return groupBy { Instant.ofEpochMilli(it.createdAtEpochMs).atZone(zone).toLocalDate() }
        .map { (date, notes) ->
            NotesSectionUi(
                dayLabel = date.toDayLabel(today),
                notes = notes.map { it.toCardUi(zone, today) },
            )
        }
}

private fun Note.toCardUi(zone: ZoneId, today: LocalDate): NoteCardUi {
    val createdAt = Instant.ofEpochMilli(createdAtEpochMs).atZone(zone)
    return NoteCardUi(
        id = id,
        title = title,
        dayLabel = createdAt.toLocalDate().toDayLabel(today),
        time = TIME_FORMATTER.format(createdAt),
        preview = body
            .replace(MARKDOWN_TOKENS, " ")
            .replace(WHITESPACE_RUNS, " ")
            .trim()
            .take(PREVIEW_MAX_CHARS),
        durationText = durationMs?.let(::formatClock),
        wordCount = countWords(transcript),
        status = status.toUi(),
    )
}

private fun LocalDate.toDayLabel(today: LocalDate): NoteDayLabelUi = when (this) {
    today -> NoteDayLabelUi.Today
    today.minusDays(1) -> NoteDayLabelUi.Yesterday
    else -> NoteDayLabelUi.Date(DAY_FORMATTER.format(this))
}

private fun countWords(text: String): Int =
    text.split(WHITESPACE_RUNS).count { it.isNotBlank() }

internal fun Note.toDetailUi(): NoteDetailUi = NoteDetailUi(
    id = id,
    title = title,
    body = body,
    transcript = transcript,
    createdAt = formatDate(createdAtEpochMs),
    hasAudio = audioFileName != null,
    metrics = toMetricsUi(),
    status = status.toUi(),
)

private fun NoteStatus.toUi(): NoteStatusUi = when (this) {
    NoteStatus.PROCESSING -> NoteStatusUi.PROCESSING
    NoteStatus.READY -> NoteStatusUi.READY
    NoteStatus.FAILED -> NoteStatusUi.FAILED
}

internal fun PcmPlaybackState.toUi(): AudioPlaybackUi = AudioPlaybackUi(
    isAvailable = isLoaded,
    isPlaying = isPlaying,
    progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    },
    positionText = formatClock(positionMs),
    durationText = formatClock(durationMs),
)

internal fun formatClock(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private fun Note.toMetricsUi(): NoteMetricsUi? {
    val transcription = transcriptionTimeMs ?: return null
    val structuring = structuringTimeMs ?: return null
    val backend = hardwareBackend ?: return null
    return NoteMetricsUi(
        transcriptionTime = DurationFormatter.format(transcription),
        structuringTime = DurationFormatter.format(structuring),
        hardwareBackend = backend,
    )
}

private fun formatDate(epochMs: Long): String =
    DATE_FORMATTER.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
