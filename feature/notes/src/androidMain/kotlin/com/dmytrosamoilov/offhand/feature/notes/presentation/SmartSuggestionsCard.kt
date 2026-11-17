package com.dmytrosamoilov.offhand.feature.notes.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.designsystem.component.BarIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.ProBadge
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.Skeleton
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandRadius
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R

@Immutable
internal data class SuggestionActions(
    val onFind: () -> Unit,
    val onAdd: (Int) -> Unit,
    val onDismiss: (Int) -> Unit,
)

@Composable
internal fun SmartSuggestionsCard(suggestions: SmartSuggestionsUi, actions: SuggestionActions, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.notes_suggestions_heading),
                style = OffhandTheme.type.buttonSmall,
                color = OffhandTheme.colors.ink,
            )
            if (suggestions == SmartSuggestionsUi.Locked) ProBadge()
        }
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SmartSuggestionsBody(suggestions = suggestions, actions = actions)
        }
    }
}

@Composable
private fun SmartSuggestionsBody(suggestions: SmartSuggestionsUi, actions: SuggestionActions) {
    when (suggestions) {
        SmartSuggestionsUi.Loading -> {
            SuggestionsStatus(text = stringResource(R.string.notes_suggestions_loading))
            Skeleton(modifier = Modifier.padding(horizontal = 18.dp), lineWidths = LOADING_LINES)
        }
        SmartSuggestionsUi.Empty -> SuggestionsStatus(text = stringResource(R.string.notes_suggestions_empty))
        SmartSuggestionsUi.NotRun -> FindSuggestionsButton(onClick = actions.onFind)
        SmartSuggestionsUi.Locked -> {
            SuggestionsStatus(text = stringResource(R.string.notes_suggestions_locked))
            FindSuggestionsButton(onClick = actions.onFind)
        }
        is SmartSuggestionsUi.Ready -> SuggestionCards(events = suggestions.events, actions = actions)
    }
}

@Composable
private fun SuggestionCards(events: List<CalendarEventUi>, actions: SuggestionActions) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .height(IntrinsicSize.Max)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        events.forEach { event ->
            key(event.index) {
                SuggestionCard(
                    event = event,
                    onAdd = { actions.onAdd(event.index) },
                    onDismiss = { actions.onDismiss(event.index) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionsStatus(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(horizontal = 18.dp),
    )
}

@Composable
private fun FindSuggestionsButton(onClick: () -> Unit) {
    SecondaryButton(
        text = stringResource(R.string.notes_suggestions_find),
        icon = OffhandIcons.Calendar,
        onClick = onClick,
        height = 48.dp,
        modifier = Modifier.padding(horizontal = 18.dp),
    )
}

@Composable
private fun SuggestionCard(event: CalendarEventUi, onAdd: () -> Unit, onDismiss: () -> Unit) {
    val colors = OffhandTheme.colors
    val shape = RoundedCornerShape(OffhandRadius.field)
    Column(
        modifier = Modifier
            .width(SUGGESTION_CARD_WIDTH)
            .fillMaxHeight()
            .clip(shape)
            .background(colors.ground)
            .border(1.dp, colors.hairline, shape)
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = event.title,
            style = OffhandTheme.type.body,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(text = event.whenLabel(), style = OffhandTheme.type.bodySmall, color = colors.ink2)
        SuggestionDetail(text = event.location)
        SuggestionDetail(text = event.details)
        Spacer(modifier = Modifier.weight(1f))
        SuggestionCardActions(isAdded = event.isAdded, onAdd = onAdd, onDismiss = onDismiss)
    }
}

@Composable
private fun CalendarEventUi.whenLabel(): String =
    if (isAllDay) "$whenText · ${stringResource(R.string.notes_calendar_all_day)}" else whenText

@Composable
private fun SuggestionDetail(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink3,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = 12.dp),
    )
}

@Composable
private fun SuggestionCardActions(isAdded: Boolean, onAdd: () -> Unit, onDismiss: () -> Unit) {
    val haptics = haptics()
    val colors = OffhandTheme.colors
    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isAdded) {
            Icon(OffhandIcons.Check, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.notes_suggestions_added),
                style = OffhandTheme.type.buttonSmall,
                color = colors.ink2,
                modifier = Modifier.padding(start = 6.dp),
            )
        } else {
            SecondaryButton(
                text = stringResource(R.string.notes_calendar_add),
                onClick = {
                    haptics.confirm()
                    onAdd()
                },
                height = 44.dp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        BarIconButton(
            icon = OffhandIcons.Close,
            contentDescription = stringResource(R.string.notes_suggestions_dismiss),
            onClick = onDismiss,
            tint = colors.ink2,
        )
    }
}

@Composable
internal fun CalendarEventLauncher(event: CalendarEventSuggestion?, onLaunched: (Boolean) -> Unit) {
    val context = LocalContext.current
    var isCalendarMissing by remember { mutableStateOf(false) }
    LaunchedEffect(event) {
        if (event == null) return@LaunchedEffect
        val opened = openCalendarInsert(context, event)
        isCalendarMissing = !opened
        onLaunched(opened)
    }
    if (isCalendarMissing) {
        NoticeDialog(
            titleRes = R.string.notes_calendar_sheet_title,
            bodyRes = R.string.notes_calendar_no_app,
            onDismiss = { isCalendarMissing = false },
        )
    }
}

private fun openCalendarInsert(context: Context, event: CalendarEventSuggestion): Boolean =
    startCalendarInsert(context, calendarInsertIntent(event).setData(CalendarContract.Events.CONTENT_URI)) ||
        startCalendarInsert(context, calendarInsertIntent(event).setType(CALENDAR_EVENT_MIME_TYPE))

private fun startCalendarInsert(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (missing: ActivityNotFoundException) {
    false
}

private fun calendarInsertIntent(event: CalendarEventSuggestion): Intent =
    Intent(Intent.ACTION_INSERT)
        .putExtra(CalendarContract.Events.TITLE, event.title)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startEpochMs)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endEpochMs)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.isAllDay)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        .putExtra(CalendarContract.Events.DESCRIPTION, event.details)

private const val CALENDAR_EVENT_MIME_TYPE = "vnd.android.cursor.item/event"
private val SUGGESTION_CARD_WIDTH = 260.dp
private val LOADING_LINES = listOf(0.8f, 0.5f)
