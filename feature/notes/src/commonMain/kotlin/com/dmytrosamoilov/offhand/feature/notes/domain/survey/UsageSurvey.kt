package com.dmytrosamoilov.offhand.feature.notes.domain.survey

// "What do you mostly record?" A changed set of answers is a new question and
// needs a new id, so old and new answers are never counted together.
object UsageSurvey {
    const val ID = "mostly_record_v1"
}

enum class UsageSurveyStep { SHOWN, ANSWER_CLICKED }

enum class UsageSurveyAnswer(val analyticsName: String) {
    MEETINGS("meetings"),
    THOUGHTS("thoughts"),
    LECTURES("lectures"),
    INTERVIEWS("interviews"),
    OTHER("other"),
}
