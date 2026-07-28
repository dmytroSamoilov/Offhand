package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPromptSetTest {

    private val promptSets = listOf(ModelPromptSet.Gemma4)

    @Test
    fun `every model family maps to its own prompt set`() {
        assertEquals(ModelPromptSet.Gemma4, ModelPromptSet.forFamily(ModelFamily.GEMMA4))
    }

    @Test
    fun `gemma structure prompt never mentions thinking blocks`() {
        NotePreset.entries.forEach { preset ->
            assertFalse(ModelPromptSet.Gemma4.structureNote(preset).contains("thinking"))
        }
    }

    @Test
    fun `all structure prompts share the json shape and factuality rules`() {
        promptSets.forEach { promptSet ->
            NotePreset.entries.forEach { preset ->
                val prompt = promptSet.structureNote(preset)
                assertTrue(prompt.contains("""{"title": "...", "overview": "..."}"""))
                assertTrue(prompt.contains("never invent or guess anything"))
                assertTrue(prompt.contains("at most 8 words"))
            }
        }
    }

    @Test
    fun `each preset prompt names every section it will be merged by`() {
        promptSets.forEach { promptSet ->
            NotePreset.entries.forEach { preset ->
                val prompt = promptSet.structureNote(preset)
                NotePresetPrompt.sections(preset).forEach { section ->
                    assertTrue("$preset misses $section", prompt.contains(section))
                }
            }
        }
    }

    @Test
    fun `sectioned presets forbid empty headings`() {
        NotePreset.entries.filter { NotePresetPrompt.sections(it).isNotEmpty() }.forEach { preset ->
            assertTrue(
                ModelPromptSet.Gemma4.structureNote(preset)
                    .contains("Never write a heading with nothing under it"),
            )
        }
    }

    @Test
    fun `summary preset asks for first person prose without lists`() {
        val prompt = ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY)

        assertTrue(prompt.contains("the speaker's own voice"))
        assertTrue(prompt.contains("Keep the first person the speaker uses"))
        assertTrue(prompt.contains("Never use headings, bullet points"))
        assertTrue(prompt.contains("Say each thing only once"))
    }

    @Test
    fun `presets do not leak each others instructions`() {
        val meeting = ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING)
        val legal = ModelPromptSet.Gemma4.structureNote(NotePreset.LEGAL)

        assertFalse(meeting.contains("## Advice given"))
        assertFalse(legal.contains("## Action items"))
    }

    @Test
    fun `all proofread prompts forbid rephrasing and invention`() {
        promptSets.map { it.proofreadTranscript }.forEach { prompt ->
            assertTrue(prompt.contains("Do not shorten, rephrase or summarize"))
            assertTrue(prompt.contains("Never invent names, dates or numbers"))
        }
    }
}
