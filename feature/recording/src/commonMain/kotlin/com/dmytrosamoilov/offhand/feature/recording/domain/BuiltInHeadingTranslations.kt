package com.dmytrosamoilov.offhand.feature.recording.domain

// How the built-in section headings read in the app's other languages (German,
// Spanish, Hindi, Portuguese, Russian, Ukrainian). The prompts give the
// headings in English and ask for them in the recording's language; this is
// how a translated heading is recognised as the section it stands for. A
// wording that is not listed still shows, as a section of its own.
internal object BuiltInHeadingTranslations {

    fun of(headings: List<String>): Map<String, List<String>> =
        headings.associateWith { TRANSLATIONS[it].orEmpty() }

    private val TRANSLATIONS: Map<String, List<String>> = mapOf(
        "## Discussion" to listOf("Diskussion", "Besprechung", "Discusión", "Debate", "चर्चा", "Discussão", "Обсуждение", "Обговорення"),
        "## Decisions" to listOf("Entscheidungen", "Beschlüsse", "Decisiones", "निर्णय", "फ़ैसले", "Decisões", "Решения", "Рішення"),
        "## Action items" to listOf("Aufgaben", "Maßnahmen", "Nächste Schritte", "Tareas", "Acciones", "Tareas pendientes", "कार्य", "कार्य बिंदु", "Tarefas", "Ações", "Itens de ação", "Задачи", "Действия", "Завдання", "Дії"),
        "## Open questions" to listOf("Offene Fragen", "Preguntas abiertas", "Preguntas pendientes", "खुले प्रश्न", "अनुत्तरित प्रश्न", "Perguntas em aberto", "Questões em aberto", "Открытые вопросы", "Відкриті питання"),
        "## Visit" to listOf("Besuch", "Visita", "मुलाक़ात", "दौरा", "विज़िट", "Визит", "Посещение", "Візит", "Відвідування"),
        "## Observations" to listOf("Beobachtungen", "Observaciones", "अवलोकन", "Observações", "Наблюдения", "Спостереження"),
        "## Actions taken" to listOf("Durchgeführte Maßnahmen", "Ergriffene Maßnahmen", "Acciones realizadas", "Medidas tomadas", "की गई कार्रवाई", "Ações realizadas", "Medidas tomadas", "Принятые меры", "Выполненные действия", "Вжиті заходи", "Виконані дії"),
        "## Follow-up" to listOf("Nachverfolgung", "Nächste Schritte", "Seguimiento", "Próximos pasos", "फ़ॉलो-अप", "आगे की कार्रवाई", "Acompanhamento", "Próximos passos", "Дальнейшие действия", "Следующие шаги", "Подальші дії", "Наступні кроки"),
        "## Matter" to listOf("Angelegenheit", "Sache", "Mandat", "Asunto", "मामला", "Assunto", "Caso", "Дело", "Вопрос", "Справа", "Питання"),
        "## Facts stated" to listOf("Geschilderte Tatsachen", "Sachverhalt", "Hechos expuestos", "Hechos", "बताए गए तथ्य", "तथ्य", "Fatos relatados", "Fatos", "Изложенные факты", "Факты", "Викладені факти", "Факти"),
        "## Instructions" to listOf("Anweisungen", "Auftrag", "Instrucciones", "निर्देश", "Instruções", "Поручения", "Инструкции", "Указания", "Доручення", "Інструкції", "Вказівки"),
        "## Advice given" to listOf("Erteilter Rat", "Beratung", "Asesoramiento", "Consejo dado", "दी गई सलाह", "Orientação dada", "Aconselhamento", "Данные рекомендации", "Рекомендации", "Надані рекомендації", "Рекомендації"),
        "## Next steps" to listOf("Nächste Schritte", "Próximos pasos", "Siguientes pasos", "अगले कदम", "Próximos passos", "Следующие шаги", "Наступні кроки"),
        "## Subject" to listOf("Betreff", "Asunto", "विषय", "Assunto", "Тема"),
        "## Email" to listOf("E-Mail", "Correo", "Correo electrónico", "ईमेल", "E-mail", "Письмо", "Электронное письмо", "Лист", "Електронний лист"),
    )
}
