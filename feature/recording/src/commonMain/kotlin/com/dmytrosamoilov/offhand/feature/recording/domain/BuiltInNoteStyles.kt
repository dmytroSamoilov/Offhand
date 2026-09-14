package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat

internal object BuiltInNoteStyles {

    fun spec(preset: NotePreset): NoteStyleSpec = when (preset) {
        NotePreset.SUMMARY -> CustomNoteStyleSpecBuilder.build(
            ref = NoteStyleRef.BuiltIn(preset),
            noteKind = SUMMARY_KIND,
            sections = SUMMARY_SECTIONS,
            language = NoteStyleLanguage.RECORDING,
        )
        else -> NoteStyleSpec(
            ref = NoteStyleRef.BuiltIn(preset),
            kind = NoteStylePrompt.markdownKind(kind(preset)),
            sections = sections(preset),
            overviewRule = overviewRule(preset),
            language = NoteStyleLanguage.RECORDING,
        )
    }

    fun sections(preset: NotePreset): List<String> = when (preset) {
        NotePreset.SUMMARY -> spec(preset).sections
        NotePreset.MEETING -> MEETING_SECTIONS
        NotePreset.VISIT -> VISIT_SECTIONS
        NotePreset.LEGAL -> LEGAL_SECTIONS
    }

    private fun kind(preset: NotePreset): String = when (preset) {
        NotePreset.SUMMARY -> SUMMARY_KIND
        NotePreset.MEETING -> MEETING_KIND
        NotePreset.VISIT -> VISIT_KIND
        NotePreset.LEGAL -> LEGAL_KIND
    }

    private fun overviewRule(preset: NotePreset): String = when (preset) {
        NotePreset.SUMMARY -> spec(preset).overviewRule
        NotePreset.MEETING -> MEETING_OVERVIEW
        NotePreset.VISIT -> VISIT_OVERVIEW
        NotePreset.LEGAL -> LEGAL_OVERVIEW
    }

    private const val SUMMARY_KIND = "a summary of the recording"
    private val SUMMARY_SECTIONS = listOf(
        NoteStyleSection(
            heading = "Main Topics",
            guidance = "list the primary subjects discussed in the recording",
            format = SectionFormat.BULLETS,
        ),
        NoteStyleSection(
            heading = "Key Decisions",
            guidance = "list all significant decisions made during the discussion",
            format = SectionFormat.BULLETS,
        ),
        NoteStyleSection(
            heading = "Action Items",
            guidance = "list all tasks that need to be completed following the recording",
            format = SectionFormat.BULLETS,
        ),
        NoteStyleSection(
            heading = "Summary Overview",
            guidance = "provide a brief, one-paragraph overview of the entire recording",
            format = SectionFormat.SENTENCES,
        ),
    )
    private const val MEETING_KIND = "meeting notes"
    private const val VISIT_KIND = "a visit report"
    private const val LEGAL_KIND = "a legal file note"

    private val MEETING_SECTIONS =
        listOf("## Discussion", "## Decisions", "## Action items", "## Open questions")
    private val VISIT_SECTIONS =
        listOf("## Visit", "## Observations", "## Actions taken", "## Follow-up")
    private val LEGAL_SECTIONS = listOf(
        "## Matter",
        "## Facts stated",
        "## Instructions",
        "## Advice given",
        "## Next steps",
    )

    private const val MEETING_OVERVIEW =
        "meeting notes in Markdown, built only from these section headings: " +
            "\"## Discussion\", \"## Decisions\", \"## Action items\", \"## Open questions\". " +
            "Every point goes under exactly one heading and is never repeated under another one. " +
            "Under \"## Discussion\" write short plain sentences for each topic that was talked " +
            "about, leaving out anything that already belongs under Decisions, Action items or " +
            "Open questions. Under \"## Decisions\" write one \"- \" line per decision that was " +
            "actually agreed. Under \"## Action items\" write one \"- \" line per task: start with " +
            "the person responsible, using the name when one is said or the word I when the speaker " +
            "takes the task, then what they have to do, then the deadline when one is said. " +
            "Under \"## Open questions\" write one \"- \" line per question that was raised and " +
            "left unanswered."

    private const val VISIT_OVERVIEW =
        "a visit report in Markdown, built only from these section headings: " +
            "\"## Visit\", \"## Observations\", \"## Actions taken\", \"## Follow-up\". " +
            "Under \"## Visit\" write who or what the visit was about and the place, date or time, " +
            "using only what is spoken. Under \"## Observations\" write one \"- \" line per reading, " +
            "measurement, symptom or condition that is reported, including symptoms the person " +
            "describes themselves, keeping the exact number and unit as spoken. " +
            "Under \"## Actions taken\" write only what was already done during the visit. " +
            "Anything that is still planned, said with will or need to, goes under Follow-up " +
            "instead. Under \"## Follow-up\" write one \"- \" line per next step, task, appointment " +
            "or thing to escalate, with who and when when it is said."

    private const val LEGAL_OVERVIEW =
        "a file note in Markdown, built only from these section headings: " +
            "\"## Matter\", \"## Facts stated\", \"## Instructions\", \"## Advice given\", " +
            "\"## Next steps\". Under \"## Matter\" write the client, case or matter and the people " +
            "involved, exactly as they are named. Under \"## Facts stated\" write one \"- \" line per " +
            "fact reported, keeping the exact dates, names, amounts and document titles as spoken. " +
            "Under \"## Instructions\" write what the client asked for. Under \"## Advice given\" " +
            "write what the speaker advised, in the speaker's own words. Under \"## Next steps\" " +
            "write one \"- \" line per task: start with who does it, using the name when one is " +
            "said or the word I when the speaker takes the task, then the task, then the deadline " +
            "as spoken. Never add a legal conclusion, opinion or classification that was not " +
            "spoken, and when something is unclear write it as it was said instead of guessing."
}
