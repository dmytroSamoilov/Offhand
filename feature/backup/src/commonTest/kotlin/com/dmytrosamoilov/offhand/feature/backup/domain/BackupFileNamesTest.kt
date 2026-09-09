package com.dmytrosamoilov.offhand.feature.backup.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupFileNamesTest {

    @Test
    fun `suggested name carries the prefix and a timestamp`() {
        val name = BackupFileNames.suggested()

        assertTrue(name.startsWith("Offhand backup "))
        assertTrue(Regex("Offhand backup \\d{4}-\\d{2}-\\d{2} \\d{2}-\\d{2}").matches(name), name)
    }

    @Test
    fun `illegal characters and whitespace runs are cleaned`() {
        assertEquals("Q3 plan review draft", BackupFileNames.sanitized("  Q3: plan / review * \"draft\"?  "))
    }

    @Test
    fun `extension is added exactly once`() {
        assertEquals("Work.offhand", BackupFileNames.withExtension("Work"))
        assertEquals("Work.offhand", BackupFileNames.withExtension("Work.offhand"))
    }

    @Test
    fun `blank name falls back to the suggestion`() {
        assertTrue(BackupFileNames.withExtension("   ").startsWith("Offhand backup "))
    }
}
