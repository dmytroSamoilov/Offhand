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
    fun `every preset with headings forbids empty headings`() {
        NotePreset.entries.filter { BuiltInNoteStyles.sections(it).isNotEmpty() }.forEach { preset ->
            assertTrue(
                ModelPromptSet.Gemma4.structureNote(spec(preset))
                    .contains("Never write a heading with nothing under it"),
            )
        }
    }

    @Test
    fun `summary preset is one to three plain sentences and the polish pass holds it to that`() {
        val prompt = ModelPromptSet.Gemma4.structureNote(spec(NotePreset.SUMMARY))
        val polish = ModelPromptSet.Gemma4.polishNote(spec(NotePreset.SUMMARY), thinkingEnabled = false)

        assertTrue(BuiltInNoteStyles.sections(NotePreset.SUMMARY).isEmpty())
        assertTrue(prompt.contains("in one to three plain sentences"))
        assertTrue(prompt.contains("with no heading, no list, no line breaks and no Markdown"))
        assertFalse(prompt.contains("Never write a heading with nothing under it"))
        assertTrue(polish.contains("one to three sentences long in total"))
        assertTrue(polish.contains("no headings, no \"- \" lines and no other Markdown"))
        assertFalse(polish.contains("Keep only these section headings"))
        assertTrue(spec(NotePreset.SUMMARY).isCondensedByPolish)
    }

    @Test
    fun `email preset asks for a subject line and a formatted body and keeps one of each`() {
        val prompt = ModelPromptSet.Gemma4.structureNote(spec(NotePreset.EMAIL))
        val polish = ModelPromptSet.Gemma4.polishNote(spec(NotePreset.EMAIL), thinkingEnabled = false)

        assertEquals(listOf("## Subject", "## Email"), BuiltInNoteStyles.sections(NotePreset.EMAIL))
        assertTrue(prompt.contains("Under \"## Subject\" write one line with the subject"))
        assertTrue(prompt.contains("a greeting line"))
        assertTrue(prompt.contains("never write a placeholder"))
        assertTrue(polish.contains("exactly one line under \"## Subject\""))
        assertEquals(listOf("## Email"), spec(NotePreset.EMAIL).paragraphSections)
    }

    @Test
    fun `built-in styles with headings ask for them in the recording's language and Summary does not`() {
        listOf(NotePreset.MEETING, NotePreset.VISIT, NotePreset.LEGAL, NotePreset.EMAIL).forEach { preset ->
            assertTrue(ModelPromptSet.Gemma4.structureNote(spec(preset)).contains("write each heading translated into that language"))
            assertTrue(ModelPromptSet.Gemma4.polishNote(spec(preset), thinkingEnabled = false).contains("never turn them back into English"))
            BuiltInNoteStyles.sections(preset).forEach { heading ->
                assertTrue(spec(preset).headingTranslations.getValue(heading).isNotEmpty(), "$heading has no translations")
            }
        }
        assertFalse(ModelPromptSet.Gemma4.structureNote(spec(NotePreset.SUMMARY)).contains("translated into that language"))
    }

    @Test
    fun `built-in list sections are named to the polish pass`() {
        val polish = ModelPromptSet.Gemma4.polishNote(spec(NotePreset.MEETING), thinkingEnabled = false)

        assertTrue(polish.contains("Keep each point as one \"- \" line under \"## Decisions\", \"## Action items\", \"## Open questions\"."))
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
        NotePreset.entries.filter { BuiltInNoteStyles.sections(it).isNotEmpty() }.forEach { preset ->
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
