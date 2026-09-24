package com.dmytrosamoilov.offhand.feature.settings.presentation

internal fun importNoticeOf(hasUnreadable: Boolean, startedCount: Int): ImportNoticeUi? = when {
    hasUnreadable -> ImportNoticeUi.Unreadable
    startedCount > 0 -> ImportNoticeUi.Started(startedCount)
    else -> null
}
