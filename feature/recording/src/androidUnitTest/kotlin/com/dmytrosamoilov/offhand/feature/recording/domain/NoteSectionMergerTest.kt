package com.dmytrosamoilov.offhand.feature.recording.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSectionMergerTest {

    private val headings = listOf("## Discussion", "## Decisions", "## Action items")

    @Test
    fun `same heading from several segments becomes one section`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf(
                "## Discussion\nBudget was reviewed.\n## Action items\n- Anna sends the draft",
                "## Discussion\nHiring was reviewed.\n## Action items\n- Bob books the room",
            ),
            headings = headings,
        )

        assertEquals(
            "## Discussion\nBudget was reviewed.\nHiring was reviewed.\n\n" +
                "## Action items\n- Anna sends the draft\n- Bob books the room",
            merged,
        )
    }

    @Test
    fun `sections are ordered by the preset and empty ones are dropped`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf("## Action items\n- Ship it\n## Discussion\nWe talked."),
            headings = headings,
        )

        assertEquals("## Discussion\nWe talked.\n\n## Action items\n- Ship it", merged)
    }

    @Test
    fun `repeated lines are written once`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf("## Decisions\n- Ship Friday", "## Decisions\n- Ship Friday"),
            headings = headings,
        )

        assertEquals("## Decisions\n- Ship Friday", merged)
    }

    @Test
    fun `heading level and casing differences still match the preset section`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf("# decisions\n- Ship Friday", "### Decisions\n- Freeze the scope"),
            headings = headings,
        )

        assertEquals("## Decisions\n- Ship Friday\n- Freeze the scope", merged)
    }

    @Test
    fun `text before any heading is kept first`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf("A short recap.\n## Decisions\n- Ship Friday"),
            headings = headings,
        )

        assertEquals("A short recap.\n\n## Decisions\n- Ship Friday", merged)
    }

    @Test
    fun `headings the model invented are kept after the preset sections`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf("## Risks\n- Vendor delay\n## Decisions\n- Ship Friday"),
            headings = headings,
        )

        assertEquals("## Decisions\n- Ship Friday\n\n## Risks\n- Vendor delay", merged)
    }

    @Test
    fun `doubled and mixed list markers collapse to a single dash`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf("## Decisions\n- - Ship Friday\n* Freeze the scope\n• - Keep onboarding"),
            headings = headings,
        )

        assertEquals(
            "## Decisions\n- Ship Friday\n- Freeze the scope\n- Keep onboarding",
            merged,
        )
    }

    @Test
    fun `a statement restated in a later specific section is dropped from the earlier one`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf(
                "## Discussion\nThe build is mostly stable.\n" +
                    "The decision was made to ship version 2.1 to the Play Store on Friday.\n" +
                    "The old onboarding will be kept for now and revisited next quarter.\n" +
                    "## Decisions\n- Ship version 2.1 to the Play Store on Friday.\n" +
                    "- Keep the old onboarding for now and revisit it next quarter.",
            ),
            headings = headings,
        )

        assertEquals(
            "## Discussion\nThe build is mostly stable.\n\n" +
                "## Decisions\n- Ship version 2.1 to the Play Store on Friday.\n" +
                "- Keep the old onboarding for now and revisit it next quarter.",
            merged,
        )
    }

    @Test
    fun `distinct statements sharing a few words are kept in both sections`() {
        val merged = NoteSectionMerger.merge(
            overviews = listOf(
                "## Discussion\nThe team talked about the release timeline for a while.\n" +
                    "## Decisions\n- The release moves to Friday.",
            ),
            headings = headings,
        )

        assertEquals(
            "## Discussion\nThe team talked about the release timeline for a while.\n\n" +
                "## Decisions\n- The release moves to Friday.",
            merged,
        )
    }

    @Test
    fun `overview without headings survives untouched`() {
        val merged = NoteSectionMerger.merge(listOf("Just a plain paragraph."), headings)

        assertEquals("Just a plain paragraph.", merged)
    }
}
