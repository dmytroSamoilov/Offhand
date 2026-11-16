package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

internal object BuiltInNoteStyles {

    fun spec(preset: NotePreset): NoteStyleSpec = when (preset) {
        NotePreset.SUMMARY -> summarySpec()
        NotePreset.EMAIL -> emailSpec()
        else -> NoteStyleSpec(
            ref = NoteStyleRef.BuiltIn(preset),
            kind = NoteStylePrompt.markdownKind(kind(preset)),
            sections = sections(preset),
            overviewRule = overviewRule(preset),
            language = NoteStyleLanguage.RECORDING,
            listSections = listSections(preset),
            headingTranslations = BuiltInHeadingTranslations.of(sections(preset)),
        )
    }

    // The free style: one to three sentences, no headings and no lists. A long
    // recording is summarised piece by piece, so the polish pass is what
    // brings the pieces down to that length.
    private fun summarySpec(): NoteStyleSpec = NoteStyleSpec(
        ref = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
        kind = SUMMARY_KIND,
        sections = emptyList(),
        overviewRule = SUMMARY_OVERVIEW,
        language = NoteStyleLanguage.RECORDING,
        polishRules = listOf(SUMMARY_POLISH_RULE),
        isCondensedByPolish = true,
    )

    private fun emailSpec(): NoteStyleSpec = NoteStyleSpec(
        ref = NoteStyleRef.BuiltIn(NotePreset.EMAIL),
        kind = NoteStylePrompt.markdownKind(EMAIL_KIND),
        sections = EMAIL_SECTIONS,
        overviewRule = EMAIL_OVERVIEW,
        language = NoteStyleLanguage.RECORDING,
        paragraphSections = listOf(EMAIL_BODY_HEADING),
        headingTranslations = BuiltInHeadingTranslations.of(EMAIL_SECTIONS),
        polishRules = listOf(EMAIL_POLISH_RULE),
    )

    fun sections(preset: NotePreset): List<String> = when (preset) {
        NotePreset.SUMMARY -> emptyList()
        NotePreset.MEETING -> MEETING_SECTIONS
        NotePreset.VISIT -> VISIT_SECTIONS
        NotePreset.LEGAL -> LEGAL_SECTIONS
        NotePreset.EMAIL -> EMAIL_SECTIONS
    }

    // The sections whose points are "- " lines, so the polish pass is told to
    // keep them as lists instead of folding them into sentences.
    private fun listSections(preset: NotePreset): List<String> = when (preset) {
        NotePreset.MEETING -> listOf("## Decisions", "## Action items", "## Open questions")
        NotePreset.VISIT -> listOf("## Observations", "## Follow-up")
        NotePreset.LEGAL -> listOf("## Facts stated", "## Next steps")
        NotePreset.SUMMARY, NotePreset.EMAIL -> emptyList()
    }

    private fun kind(preset: NotePreset): String = when (preset) {
        NotePreset.SUMMARY -> SUMMARY_KIND
        NotePreset.MEETING -> MEETING_KIND
        NotePreset.VISIT -> VISIT_KIND
        NotePreset.LEGAL -> LEGAL_KIND
        NotePreset.EMAIL -> EMAIL_KIND
    }

    private fun overviewRule(preset: NotePreset): String = when (preset) {
        NotePreset.SUMMARY -> SUMMARY_OVERVIEW
        NotePreset.MEETING -> MEETING_OVERVIEW
        NotePreset.VISIT -> VISIT_OVERVIEW
        NotePreset.LEGAL -> LEGAL_OVERVIEW
        NotePreset.EMAIL -> EMAIL_OVERVIEW
    }

    private const val SUMMARY_KIND = "a short summary of the recording, written as plain running text"
    private const val SUMMARY_OVERVIEW =
        "a summary of the whole recording in one to three plain sentences: what it is about and the " +
            "most important decision, task or conclusion. Write it as one paragraph of running text, " +
            "with no heading, no list, no line breaks and no Markdown."
    private const val SUMMARY_POLISH_RULE =
        "- The finished note is one to three sentences long in total, however long the draft is: " +
            "merge the draft into one paragraph that says what the recording is about and keeps only " +
            "the most important decision, task or conclusion."

    private const val EMAIL_KIND = "an email that is ready to send"
    private const val EMAIL_SUBJECT_HEADING = "## Subject"
    private const val EMAIL_BODY_HEADING = "## Email"
    private val EMAIL_SECTIONS = listOf(EMAIL_SUBJECT_HEADING, EMAIL_BODY_HEADING)
    private const val EMAIL_OVERVIEW =
        "an email the speaker can send as it is, in Markdown, built from exactly these two section " +
            "headings in this order: \"## Subject\", \"## Email\". Under \"## Subject\" write one " +
            "line with the subject of the email, at most 10 words, with no full stop at the end. " +
            "Under \"## Email\" write the body in the first person, as the speaker: first a greeting " +
            "line that uses the recipient's name when one is said, then an empty line, then the " +
            "message in short paragraphs with an empty line between them, then an empty line and a " +
            "closing line. Use \"- \" lines only for a list of separate items such as dates, tasks " +
            "or questions. A greeting and a closing line are part of every email and may be written " +
            "even though nobody speaks them; everything else comes only from the recording. End " +
            "with the sender's name only when the speaker says their name, and never write a " +
            "placeholder such as [Name] or [Your name]. Leave out filler words, false starts and " +
            "remarks about the recording itself."
    private const val EMAIL_POLISH_RULE =
        "- The finished note is one email: exactly one line under \"## Subject\", and under " +
            "\"## Email\" one greeting line, the message in short paragraphs separated by empty " +
            "lines, and one closing line. When the draft holds several subjects, greetings or " +
            "closings, keep one of each."
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
