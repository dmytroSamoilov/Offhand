@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraft
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraftException
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleErrors
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidation
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleValidator
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.DraftNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.GetCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.PreviewNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SaveCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SaveNoteStyleResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteStyleEditorViewModel(
    private val styleId: Long,
    private val getCustomNoteStyle: GetCustomNoteStyleUseCase,
    private val saveCustomNoteStyle: SaveCustomNoteStyleUseCase,
    private val previewNoteStyle: PreviewNoteStyleUseCase,
    private val draftNoteStyle: DraftNoteStyleUseCase,
    isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase,
    private val proUpgradeGate: ProUpgradeGate,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(NoteStyleEditorUiState(isNew = styleId == NEW_STYLE_ID))
    val uiState: StateFlow<NoteStyleEditorUiState> = mutableUiState.asStateFlow()

    private var createdAtEpochMs = Clock.System.now().toEpochMilliseconds()

    init {
        if (styleId != NEW_STYLE_ID) loadExisting()
        viewModelScope.launch {
            isCustomNoteStylesAvailable().collect { unlocked ->
                mutableUiState.update { it.copy(isLocked = !unlocked) }
            }
        }
    }

    private fun loadExisting() {
        launchSafely(showLoading = false) {
            val style = getCustomNoteStyle(styleId) ?: return@launchSafely
            createdAtEpochMs = style.createdAtEpochMs
            mutableUiState.update { it.copy(isNew = false).withStyle(style) }
        }
    }

    fun onNameChanged(name: String) = edit { copy(name = name, errors = errors.copy(name = null)) }

    fun onNoteKindChanged(noteKind: String) = edit { copy(noteKind = noteKind, errors = errors.copy(noteKind = null)) }

    fun onLanguageChanged(language: NoteStyleLanguage) = edit { copy(language = language) }

    fun onSectionHeadingChanged(index: Int, heading: String) = editSection(index) { copy(heading = heading) }

    fun onSectionGuidanceChanged(index: Int, guidance: String) = editSection(index) { copy(guidance = guidance) }

    fun onSectionFormatChanged(index: Int, format: SectionFormat) = editSection(index) { copy(format = format) }

    fun onSectionAdded() = edit {
        if (sections.size >= NoteStyleValidator.MAX_SECTIONS) this else copy(sections = sections + SectionDraftUi())
    }

    fun onSectionRemoved(index: Int) = edit {
        copy(sections = sections.filterIndexed { position, _ -> position != index }, errors = NoteStyleErrors())
    }

    fun onSectionMoved(from: Int, to: Int) = edit {
        if (to !in sections.indices || from !in sections.indices) return@edit this
        val moved = sections.toMutableList().apply { add(to, removeAt(from)) }
        copy(sections = moved, errors = NoteStyleErrors())
    }

    fun onPreviewRequested(sampleTranscript: String) {
        val style = validatedStyle() ?: return
        mutableUiState.update { it.copy(preview = StylePreviewUi.Running) }
        launchSafely(showLoading = false) {
            mutableUiState.update { it.copy(preview = runPreview(style, sampleTranscript)) }
        }
    }

    fun onPreviewDismissed() = edit { copy(preview = null) }

    fun onDescribeRequested() = edit { copy(describe = describe ?: DescribeStyleUi()) }

    fun onDescribeDismissed() = edit { copy(describe = null) }

    fun onDescriptionChanged(description: String) = edit {
        copy(describe = DescribeStyleUi(description = description))
    }

    fun onDraftRequested() {
        val description = mutableUiState.value.describe?.description?.trim().orEmpty()
        if (description.isEmpty()) return
        edit { copy(describe = DescribeStyleUi(description, DescribeStatusUi.RUNNING)) }
        launchSafely(showLoading = false) {
            applyDraft(description, runDraft(description))
        }
    }

    private suspend fun runDraft(description: String): NoteStyleDraft? = try {
        draftNoteStyle(description)
    } catch (backendFailure: AiBackendException) {
        null.also { edit { copy(describe = DescribeStyleUi(description, DescribeStatusUi.FAILED)) } }
    } catch (unusable: NoteStyleDraftException) {
        null.also { edit { copy(describe = DescribeStyleUi(description, DescribeStatusUi.FAILED)) } }
    }

    private fun applyDraft(description: String, draft: NoteStyleDraft?) {
        val status = mutableUiState.value.describe?.status
        when {
            draft != null -> edit { withDraft(draft).copy(describe = null, errors = NoteStyleErrors(), preview = null) }
            status == DescribeStatusUi.RUNNING ->
                edit { copy(describe = DescribeStyleUi(description, DescribeStatusUi.MODEL_UNAVAILABLE)) }
        }
    }

    private fun NoteStyleEditorUiState.withDraft(draft: NoteStyleDraft): NoteStyleEditorUiState = copy(
        name = draft.name.ifBlank { name },
        noteKind = draft.noteKind,
        sections = draft.sections.map { SectionDraftUi(it.heading, it.guidance, it.format) },
    )

    // Free users build and try the style; saving is where the paywall sits,
    // and the draft survives it either way.
    fun onSaveRequested() {
        val style = validatedStyle() ?: return
        launchSafely(showLoading = false) {
            if (!proUpgradeGate.requirePro(ProFeature.CUSTOM_STYLES)) return@launchSafely
            when (val result = saveCustomNoteStyle(style)) {
                is SaveNoteStyleResult.Saved -> mutableUiState.update { it.copy(isSaved = true) }
                is SaveNoteStyleResult.Invalid -> mutableUiState.update { it.copy(errors = result.errors) }
            }
        }
    }

    private suspend fun runPreview(style: CustomNoteStyle, sampleTranscript: String): StylePreviewUi = try {
        previewNoteStyle(style, sampleTranscript)
            ?.let { StylePreviewUi.Ready(title = it.title, overview = it.overview) }
            ?: StylePreviewUi.ModelUnavailable
    } catch (backendFailure: AiBackendException) {
        StylePreviewUi.Failed
    }

    private fun validatedStyle(): CustomNoteStyle? =
        when (val validation = NoteStyleValidator.validate(currentStyle(), existing = emptyList())) {
            is NoteStyleValidation.Valid -> validation.style
            is NoteStyleValidation.Invalid -> {
                mutableUiState.update { it.copy(errors = validation.errors) }
                null
            }
        }

    private fun currentStyle(): CustomNoteStyle = mutableUiState.value.let { state ->
        CustomNoteStyle(
            id = if (state.isNew) NEW_STYLE_ID else styleId,
            name = state.name,
            noteKind = state.noteKind,
            language = state.language,
            sections = state.sections.map { NoteStyleSection(it.heading, it.guidance, it.format) },
            createdAtEpochMs = createdAtEpochMs,
        )
    }

    private fun NoteStyleEditorUiState.withStyle(style: CustomNoteStyle): NoteStyleEditorUiState = copy(
        name = style.name,
        noteKind = style.noteKind,
        language = style.language,
        sections = style.sections.map { SectionDraftUi(it.heading, it.guidance, it.format) },
    )

    private fun edit(transform: NoteStyleEditorUiState.() -> NoteStyleEditorUiState) {
        mutableUiState.update { it.transform() }
    }

    private fun editSection(index: Int, transform: SectionDraftUi.() -> SectionDraftUi) = edit {
        val updated = sections.mapIndexed { position, section -> if (position == index) section.transform() else section }
        copy(
            sections = updated,
            errors = errors.copy(headings = errors.headings - index),
        )
    }

    companion object {
        const val NEW_STYLE_ID = 0L
    }
}
