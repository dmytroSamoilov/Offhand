package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleFieldError
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveCustomNoteStyleUseCaseTest {

    private val repository: CustomNoteStylesRepository = mockk()
    private val useCase = SaveCustomNoteStyleUseCase(repository)

    private val style = CustomNoteStyle(
        id = 0,
        name = "Sales debrief",
        noteKind = "",
        language = NoteStyleLanguage.RECORDING,
        sections = listOf(NoteStyleSection("Customer", "who they are", SectionFormat.SENTENCES)),
        createdAtEpochMs = 1,
    )

    @Test
    fun `new style is created after validation`() = runTest {
        every { repository.observeStyles() } returns flowOf(emptyList())
        coEvery { repository.createStyle(any()) } returns 9L

        val result = useCase(style.copy(name = " Sales debrief "))

        assertEquals(SaveNoteStyleResult.Saved(9L), result)
        coVerify { repository.createStyle(style) }
    }

    @Test
    fun `existing style is updated in place`() = runTest {
        val stored = style.copy(id = 4)
        every { repository.observeStyles() } returns flowOf(listOf(stored))
        coJustRun { repository.updateStyle(any()) }

        val result = useCase(stored.copy(noteKind = "a debrief"))

        assertEquals(SaveNoteStyleResult.Saved(4L), result)
        coVerify { repository.updateStyle(stored.copy(noteKind = "a debrief")) }
    }

    @Test
    fun `invalid style is rejected without touching the repository`() = runTest {
        every { repository.observeStyles() } returns flowOf(listOf(style.copy(id = 3)))

        val result = useCase(style) as SaveNoteStyleResult.Invalid

        assertEquals(NoteStyleFieldError.DUPLICATE, result.errors.name)
        coVerify(exactly = 0) { repository.createStyle(any()) }
    }
}
