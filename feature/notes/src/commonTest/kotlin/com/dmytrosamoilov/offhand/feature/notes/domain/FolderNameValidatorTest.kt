package com.dmytrosamoilov.offhand.feature.notes.domain

import com.dmytrosamoilov.offhand.core.data.domain.Folder
import kotlin.test.Test
import kotlin.test.assertEquals

class FolderNameValidatorTest {

    private val existing = listOf(
        Folder(id = 1, name = "Work", createdAtEpochMs = 0),
        Folder(id = 2, name = "Clients", createdAtEpochMs = 0),
    )

    @Test
    fun `trims and collapses whitespace`() {
        val result = FolderNameValidator.validate("  Board   meetings ", existing)

        assertEquals(FolderNameValidation.Valid("Board meetings"), result)
    }

    @Test
    fun `blank name is rejected`() {
        assertEquals(FolderNameValidation.Invalid(FolderNameError.BLANK), FolderNameValidator.validate("   ", existing))
    }

    @Test
    fun `name over the limit is rejected`() {
        val tooLong = "a".repeat(FolderNameValidator.MAX_LENGTH + 1)

        assertEquals(FolderNameValidation.Invalid(FolderNameError.TOO_LONG), FolderNameValidator.validate(tooLong, existing))
    }

    @Test
    fun `duplicate is detected case insensitively`() {
        assertEquals(FolderNameValidation.Invalid(FolderNameError.DUPLICATE), FolderNameValidator.validate("work", existing))
    }

    @Test
    fun `renaming a folder to its own name is allowed`() {
        val result = FolderNameValidator.validate("Work", existing, excludingId = 1)

        assertEquals(FolderNameValidation.Valid("Work"), result)
    }

    @Test
    fun `renaming to another folder's name is rejected`() {
        val result = FolderNameValidator.validate("Clients", existing, excludingId = 1)

        assertEquals(FolderNameValidation.Invalid(FolderNameError.DUPLICATE), result)
    }
}
