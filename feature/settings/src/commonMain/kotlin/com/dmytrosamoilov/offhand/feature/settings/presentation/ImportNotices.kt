package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioResult

internal fun importNoticeOf(hasUnreadable: Boolean, startedCount: Int, result: ImportAudioResult? = null): ImportNoticeUi? = when {
    result == ImportAudioResult.ONE_VIDEO_AT_A_TIME -> ImportNoticeUi.OneVideoAtATime
    hasUnreadable -> ImportNoticeUi.Unreadable
    startedCount > 0 -> ImportNoticeUi.Started(startedCount)
    else -> null
}
