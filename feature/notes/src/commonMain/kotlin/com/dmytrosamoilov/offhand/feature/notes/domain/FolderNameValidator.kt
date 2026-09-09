package com.dmytrosamoilov.offhand.feature.notes.domain

import com.dmytrosamoilov.offhand.core.data.domain.Folder

enum class FolderNameError {
    BLANK,
    TOO_LONG,
    DUPLICATE,
}

sealed interface FolderNameValidation {
    data class Valid(val name: String) : FolderNameValidation
    data class Invalid(val error: FolderNameError) : FolderNameValidation
}

object FolderNameValidator {

    const val MAX_LENGTH = 40

    fun validate(raw: String, existing: List<Folder>, excludingId: Long? = null): FolderNameValidation {
        val name = raw.trim().replace(WHITESPACE_RUNS, " ")
        return when {
            name.isEmpty() -> FolderNameValidation.Invalid(FolderNameError.BLANK)
            name.length > MAX_LENGTH -> FolderNameValidation.Invalid(FolderNameError.TOO_LONG)
            existing.any { it.id != excludingId && it.name.equals(name, ignoreCase = true) } ->
                FolderNameValidation.Invalid(FolderNameError.DUPLICATE)
            else -> FolderNameValidation.Valid(name)
        }
    }

    private val WHITESPACE_RUNS = Regex("\\s+")
}
