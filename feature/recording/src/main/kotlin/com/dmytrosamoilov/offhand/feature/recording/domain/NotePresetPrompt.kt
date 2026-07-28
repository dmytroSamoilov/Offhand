package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset

internal object NotePresetPrompt {

    fun fieldRules(preset: NotePreset): String = listOfNotNull(
        FIELD_RULES_HEADER,
        TITLE_RULE,
        "$OVERVIEW_RULE_PREFIX${overviewRule(preset)}",
        EMPTY_SECTION_RULE.takeIf { sections(preset).isNotEmpty() },
    ).joinToString(LINE_BREAK)

    fun sections(preset: NotePreset): List<String> = when (preset) {
        NotePreset.SUMMARY -> emptyList()
        NotePreset.MEETING -> MEETING_SECTIONS
        NotePreset.VISIT -> VISIT_SECTIONS
        NotePreset.LEGAL -> LEGAL_SECTIONS
    }

    private fun overviewRule(preset: NotePreset): String = when (preset) {
        NotePreset.SUMMARY -> SUMMARY_OVERVIEW
        NotePreset.MEETING -> MEETING_OVERVIEW
        NotePreset.VISIT -> VISIT_OVERVIEW
        NotePreset.LEGAL -> LEGAL_OVERVIEW
    }

    private const val LINE_BREAK = "\n"
    private const val FIELD_RULES_HEADER = "Rules for the fields:"
    private const val TITLE_RULE =
        "- \"title\": a short title for the recording, at most 8 words."
    private const val OVERVIEW_RULE_PREFIX = "- \"overview\": "
    private const val EMPTY_SECTION_RULE =
        "- Write a heading only when the recording really contains that kind of content. " +
            "Never write a heading with nothing under it, and never write none, not mentioned or N/A."

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

    private const val SUMMARY_OVERVIEW =
        "the recording written down again in the speaker's own voice. " +
            "Keep the first person the speaker uses: write \"I have been struggling with the build\", " +
            "never \"The speaker has been struggling with the build\". " +
            "Say each thing only once. When a thought is repeated, restarted or said again in other " +
            "words, keep the clearest version and drop the rest. When many sentences are spent on " +
            "one point, write that point in one or two sentences that still carry the specific " +
            "details: names, dates, numbers, amounts, decisions and what the speaker wants to do next. " +
            "Tidy up half-finished and rambling sentences, but keep the speaker's own words and tone, " +
            "and never add anything that was not said. " +
            "Write plain sentences in short paragraphs, in the order the topics came up. " +
            "Never use headings, bullet points, dashes at the start of a line, or numbered lists. " +
            "Make it as long as the content needs and no longer."

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
