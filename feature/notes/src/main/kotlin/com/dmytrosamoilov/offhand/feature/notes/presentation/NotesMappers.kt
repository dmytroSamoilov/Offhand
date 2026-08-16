@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.ai.api.AiCoreDownloadState
import com.dmytrosamoilov.offhand.core.audio.PcmPlaybackState
import com.dmytrosamoilov.offhand.core.common.DurationFormatter
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareBundle
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private val MARKDOWN_TOKENS = Regex("[#*>`_\\[\\]]")
private val WHITESPACE_RUNS = Regex("\\s+")
private const val PREVIEW_MAX_CHARS = 220

internal fun List<Note>.toSectionsUi(dateLabelFormatter: DateLabelFormatter): List<NotesSectionUi> {
    val zone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(zone).date
    return groupBy { Instant.fromEpochMilliseconds(it.createdAtEpochMs).toLocalDateTime(zone).date }
        .map { (date, notes) ->
            NotesSectionUi(
                dayLabel = date.toDayLabel(today, dateLabelFormatter),
                notes = notes.map { it.toCardUi(zone, today, dateLabelFormatter) },
            )
        }
}

private fun Note.toCardUi(
    zone: TimeZone,
    today: LocalDate,
    dateLabelFormatter: DateLabelFormatter,
): NoteCardUi {
    val createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs).toLocalDateTime(zone)
    return NoteCardUi(
        id = id,
        title = title,
        dayLabel = createdAt.date.toDayLabel(today, dateLabelFormatter),
        time = dateLabelFormatter.time(createdAt),
        preview = body
            .replace(MARKDOWN_TOKENS, " ")
            .replace(WHITESPACE_RUNS, " ")
            .trim()
            .take(PREVIEW_MAX_CHARS),
        durationText = durationMs?.let(::formatClock),
        status = status.toUi(),
    )
}

private fun LocalDate.toDayLabel(
    today: LocalDate,
    dateLabelFormatter: DateLabelFormatter,
): NoteDayLabelUi = when (this) {
    today -> NoteDayLabelUi.Today
    today.minus(1, DateTimeUnit.DAY) -> NoteDayLabelUi.Yesterday
    else -> NoteDayLabelUi.Date(dateLabelFormatter.day(this))
}

private fun countWords(text: String): Int =
    text.split(WHITESPACE_RUNS).count { it.isNotBlank() }

internal fun Note.toDetailUi(dateLabelFormatter: DateLabelFormatter): NoteDetailUi = NoteDetailUi(
    id = id,
    title = title,
    body = body,
    transcript = transcript,
    createdAt = dateLabelFormatter.dateTime(createdAtLocalDateTime()),
    wordCount = countWords(transcript),
    hasAudio = audioFileName != null,
    metrics = toMetricsUi(),
    status = status.toUi(),
    preset = preset,
)

private fun Note.createdAtLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(createdAtEpochMs).toLocalDateTime(TimeZone.currentSystemDefault())

private fun NoteStatus.toUi(): NoteStatusUi = when (this) {
    NoteStatus.RECORDING -> NoteStatusUi.PROCESSING
    NoteStatus.PROCESSING -> NoteStatusUi.PROCESSING
    NoteStatus.READY -> NoteStatusUi.READY
    NoteStatus.FAILED -> NoteStatusUi.FAILED
}

internal fun AiCoreDownloadState.toPreparationUi(): ModelPreparationUi? = when (this) {
    is AiCoreDownloadState.Downloading -> ModelPreparationUi(progressPercent = progressPercent)
    is AiCoreDownloadState.Idle -> null
}

internal fun NoteShareBundle.toUi(): NoteShareUi = NoteShareUi(
    filePaths = filePaths,
    mimeType = mimeType,
)

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
