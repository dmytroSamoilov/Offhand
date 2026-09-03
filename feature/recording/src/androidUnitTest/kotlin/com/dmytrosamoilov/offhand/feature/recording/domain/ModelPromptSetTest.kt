package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPromptSetTest {

    private val promptSets = listOf(ModelPromptSet.Gemma4)

    private val QUOTED_TEXT = Regex("\"([^\"]+)\"")

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
    fun `polish prompt mentions a thinking block only when thinking is enabled`() {
        NotePreset.entries.forEach { preset ->
            val thinking = ModelPromptSet.Gemma4.polishNote(preset, thinkingEnabled = true)
            assertTrue(thinking.contains("<thinking></thinking>"))
            assertTrue(thinking.contains("After the thinking block"))

            val plain = ModelPromptSet.Gemma4.polishNote(preset, thinkingEnabled = false)
            assertFalse(plain.contains("thinking"))
            assertTrue(plain.contains("Output a single JSON object and nothing else"))
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

    // Models copy quoted first-person sentences straight into the note as if
    // they had been spoken, so prompts may only quote headings, never content.
    @Test
    fun `no preset prompt quotes a first-person example sentence`() {
        NotePreset.entries.forEach { preset ->
            val prompts = listOf(
                ModelPromptSet.Gemma4.structureNote(preset),
                ModelPromptSet.Gemma4.polishNote(preset, thinkingEnabled = false),
                ModelPromptSet.Gemma4.polishNote(preset, thinkingEnabled = true),
            )
            prompts.flatMap { QUOTED_TEXT.findAll(it) }.map { it.groupValues[1] }.forEach { quoted ->
                assertFalse(
                    "$preset quotes a copyable sentence: $quoted",
                    quoted.startsWith("I ") || quoted.startsWith("The speaker "),
                )
            }
        }
    }

    @Test
    fun `all polish prompts share the json shape and the polish tasks`() {
        promptSets.forEach { promptSet ->
            NotePreset.entries.forEach { preset ->
                listOf(false, true).forEach { thinking ->
                    val prompt = promptSet.polishNote(preset, thinking)
                    assertTrue(prompt.contains("""{"title": "...", "overview": "..."}"""))
                    assertTrue(prompt.contains("at most 8 words"))
                    assertTrue(prompt.contains("Say each thing only once"))
                    assertTrue(prompt.contains("word that was most likely spoken"))
                    assertTrue(prompt.contains("never add anything the draft does not say"))
                }
            }
        }
    }

    @Test
    fun `polish prompt explains the note kind of its preset`() {
        NotePreset.entries.forEach { preset ->
            val prompt = ModelPromptSet.Gemma4.polishNote(preset, thinkingEnabled = false)
            assertTrue(prompt.contains(NotePresetPrompt.noteKind(preset)))
            NotePresetPrompt.sections(preset).forEach { section ->
                assertTrue("$preset polish prompt misses $section", prompt.contains(section))
            }
        }
    }

    @Test
    fun `summary polish prompt keeps prose and forbids lists`() {
        val prompt = ModelPromptSet.Gemma4.polishNote(NotePreset.SUMMARY, thinkingEnabled = false)

        assertTrue(prompt.contains("first person"))
        assertTrue(prompt.contains("Never use headings, bullet points"))
    }

    @Test
    fun `sectioned polish prompts allow adding a missing allowed heading`() {
        NotePreset.entries.filter { NotePresetPrompt.sections(it).isNotEmpty() }.forEach { preset ->
            assertTrue(
                "$preset polish prompt must allow adding a missing heading",
                ModelPromptSet.Gemma4.polishNote(preset, thinkingEnabled = false)
                    .contains("add that heading"),
            )
        }
        assertFalse(
            ModelPromptSet.Gemma4.polishNote(NotePreset.SUMMARY, thinkingEnabled = false)
                .contains("add that heading"),
        )
    }

    @Test
    fun `presets do not leak each others instructions`() {
        val meeting = ModelPromptSet.Gemma4.structureNote(NotePreset.MEETING)
        val legal = ModelPromptSet.Gemma4.structureNote(NotePreset.LEGAL)

        assertFalse(meeting.contains("## Advice given"))
        assertFalse(legal.contains("## Action items"))
    }
}
