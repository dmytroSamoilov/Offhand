package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ModelPromptSetTest {

    private val promptSets = listOf(ModelPromptSet.Gemma4)

    private val QUOTED_TEXT = Regex("\"([^\"]+)\"")

    private fun spec(preset: NotePreset): NoteStyleSpec = BuiltInNoteStyles.spec(preset)

    @Test
    fun `every model family maps to its own prompt set`() {
        assertEquals(ModelPromptSet.Gemma4, ModelPromptSet.forFamily(ModelFamily.GEMMA4))
    }

    @Test
    fun `gemma structure prompt never mentions thinking blocks`() {
        NotePreset.entries.forEach { preset ->
            assertFalse(ModelPromptSet.Gemma4.structureNote(spec(preset)).contains("thinking"))
        }
    }

    @Test
    fun `polish prompt mentions a thinking block only when thinking is enabled`() {
        NotePreset.entries.forEach { preset ->
            val thinking = ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = true)
            assertTrue(thinking.contains("<thinking></thinking>"))
            assertTrue(thinking.contains("After the thinking block"))

            val plain = ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = false)
            assertFalse(plain.contains("thinking"))
            assertTrue(plain.contains("Output a single JSON object and nothing else"))
        }
    }

    @Test
    fun `all structure prompts share the json shape and factuality rules`() {
        promptSets.forEach { promptSet ->
            NotePreset.entries.forEach { preset ->
                val prompt = promptSet.structureNote(spec(preset))
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
                val prompt = promptSet.structureNote(spec(preset))
                BuiltInNoteStyles.sections(preset).forEach { section ->
                    assertTrue(prompt.contains(section), "$preset misses $section")
                }
            }
        }
    }

    @Test
    fun `every preset forbids empty headings`() {
        NotePreset.entries.forEach { preset ->
            assertTrue(
                ModelPromptSet.Gemma4.structureNote(spec(preset))
                    .contains("Never write a heading with nothing under it"),
            )
        }
    }

    @Test
    fun `summary preset is the four-section quick summary`() {
        val prompt = ModelPromptSet.Gemma4.structureNote(spec(NotePreset.SUMMARY))

        assertEquals(
            listOf("## Main Topics", "## Key Decisions", "## Action Items", "## Summary Overview"),
            BuiltInNoteStyles.sections(NotePreset.SUMMARY),
        )
        assertTrue(prompt.contains("a summary of the recording in Markdown"))
        assertTrue(prompt.contains("Under \"## Action Items\" write one \"- \" line per point: list all tasks"))
        assertTrue(prompt.contains("Under \"## Summary Overview\" write short plain sentences: provide a brief, one-paragraph overview"))
    }

    // Models copy quoted first-person sentences straight into the note as if
    // they had been spoken, so prompts may only quote headings, never content.
    @Test
    fun `no preset prompt quotes a first-person example sentence`() {
        NotePreset.entries.forEach { preset ->
            val prompts = listOf(
                ModelPromptSet.Gemma4.structureNote(spec(preset)),
                ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = false),
                ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = true),
            )
            prompts.flatMap { QUOTED_TEXT.findAll(it) }.map { it.groupValues[1] }.forEach { quoted ->
                assertFalse(
                    quoted.startsWith("I ") || quoted.startsWith("The speaker "),
                    "$preset quotes a copyable sentence: $quoted",
                )
            }
        }
    }

    @Test
    fun `all polish prompts share the json shape and the polish tasks`() {
        promptSets.forEach { promptSet ->
            NotePreset.entries.forEach { preset ->
                listOf(false, true).forEach { thinking ->
                    val prompt = promptSet.polishNote(spec(preset), thinking)
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
            val prompt = ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = false)
            assertTrue(prompt.contains(spec(preset).kind))
            BuiltInNoteStyles.sections(preset).forEach { section ->
                assertTrue(prompt.contains(section), "$preset polish prompt misses $section")
            }
        }
    }

    @Test
    fun `polish prompts allow adding a missing allowed heading`() {
        NotePreset.entries.forEach { preset ->
            assertTrue(
                ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = false)
                    .contains("add that heading"),
                "$preset polish prompt must allow adding a missing heading",
            )
        }
    }

    @Test
    fun `presets do not leak each others instructions`() {
        val meeting = ModelPromptSet.Gemma4.structureNote(spec(NotePreset.MEETING))
        val legal = ModelPromptSet.Gemma4.structureNote(spec(NotePreset.LEGAL))

        assertFalse(meeting.contains("## Advice given"))
        assertFalse(legal.contains("## Action items"))
    }
}
