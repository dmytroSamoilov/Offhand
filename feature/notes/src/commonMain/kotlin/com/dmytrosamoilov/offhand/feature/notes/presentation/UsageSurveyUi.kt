package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurveyAnswer

// The question card on the notes list: it asks, opens a sheet with the
// choices, and says thanks once before it leaves for good.
data class UsageSurveyUi(
    val stage: UsageSurveyStageUi = UsageSurveyStageUi.QUESTION,
    val isSheetVisible: Boolean = false,
    val selection: Set<UsageSurveyAnswerUi> = emptySet(),
)

enum class UsageSurveyStageUi {
    QUESTION,
    THANKS,
}

enum class UsageSurveyAnswerUi {
    MEETINGS,
    THOUGHTS,
    LECTURES,
    INTERVIEWS,
    OTHER,
}

internal fun UsageSurveyAnswerUi.toDomain(): UsageSurveyAnswer = when (this) {
    UsageSurveyAnswerUi.MEETINGS -> UsageSurveyAnswer.MEETINGS
    UsageSurveyAnswerUi.THOUGHTS -> UsageSurveyAnswer.THOUGHTS
    UsageSurveyAnswerUi.LECTURES -> UsageSurveyAnswer.LECTURES
    UsageSurveyAnswerUi.INTERVIEWS -> UsageSurveyAnswer.INTERVIEWS
    UsageSurveyAnswerUi.OTHER -> UsageSurveyAnswer.OTHER
}

// A thank-you that is on screen stays until it is closed, although the
// question is no longer due the moment its answer is stored.
internal fun UsageSurveyUi?.onPromptChanged(isDue: Boolean): UsageSurveyUi? = when {
    this?.stage == UsageSurveyStageUi.THANKS -> this
    isDue -> this ?: UsageSurveyUi()
    else -> null
}
