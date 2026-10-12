package com.dmytrosamoilov.offhand.feature.recording.domain

// What the plan lets a user import right now: Pro has no limit, the free
// tier a few files for the life of the install, one at a time.
sealed interface ImportAllowance {
    data object Unlimited : ImportAllowance
    data class Free(val left: Int) : ImportAllowance

    fun covers(fileCount: Int): Boolean = when (this) {
        Unlimited -> true
        is Free -> fileCount == 1 && left > 0
    }
}

object FreeImportLimits {
    const val FREE_IMPORTS = 3
}
