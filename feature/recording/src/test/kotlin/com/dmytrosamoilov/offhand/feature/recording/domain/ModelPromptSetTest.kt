package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPromptSetTest {

    @Test
    fun `every model family maps to its own prompt set`() {
        assertEquals(ModelPromptSet.Qwen3, ModelPromptSet.forFamily(ModelFamily.QWEN3))
        assertEquals(ModelPromptSet.Gemma3n, ModelPromptSet.forFamily(ModelFamily.GEMMA3N))
    }

    @Test
    fun `qwen structure prompt requests a thinking block`() {
        assertTrue(ModelPromptSet.Qwen3.structureNote.contains("<thinking>"))
    }

    @Test
    fun `gemma structure prompt never mentions thinking blocks`() {
        assertFalse(ModelPromptSet.Gemma3n.structureNote.contains("thinking"))
    }

    @Test
    fun `all structure prompts share the json shape and factuality rules`() {
        listOf(ModelPromptSet.Qwen3, ModelPromptSet.Gemma3n).forEach { promptSet ->
            assertTrue(promptSet.structureNote.contains("""{"title": "...", "overview": "..."}"""))
            assertTrue(promptSet.structureNote.contains("never invent or guess anything"))
        }
    }

    @Test
    fun `all proofread prompts forbid rephrasing and invention`() {
        listOf(ModelPromptSet.Qwen3, ModelPromptSet.Gemma3n).forEach { promptSet ->
            assertTrue(promptSet.proofreadTranscript.contains("Do not shorten, rephrase or summarize"))
            assertTrue(promptSet.proofreadTranscript.contains("Never invent names, dates or numbers"))
        }
    }
}
