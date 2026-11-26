package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SurfaceCard
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.component.isReducedMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R
import kotlinx.coroutines.delay

@Composable
internal fun UsageSurveyCard(
    survey: UsageSurveyUi?,
    onAnswer: () -> Unit,
    onNotNow: () -> Unit,
    onThanksClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastVisible by remember { mutableStateOf(UsageSurveyStageUi.QUESTION) }
    if (survey != null) lastVisible = survey.stage
    AnimatedVisibility(
        visible = survey != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        when (lastVisible) {
            UsageSurveyStageUi.QUESTION -> QuestionCard(onAnswer = onAnswer, onNotNow = onNotNow)
            UsageSurveyStageUi.THANKS -> ThanksCard(onClosed = onThanksClosed)
        }
    }
}

@Composable
private fun QuestionCard(onAnswer: () -> Unit, onNotNow: () -> Unit) {
    SurfaceCard(modifier = Modifier.clickable(role = Role.Button, onClick = onAnswer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)) {
            SurveyCardText(
                title = stringResource(R.string.notes_usage_survey_title),
                body = stringResource(R.string.notes_usage_survey_body),
            )
            Row(
                modifier = Modifier.padding(top = 4.dp).offset(x = -OffhandSpacing.controls),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PlainTextButton(text = stringResource(R.string.notes_usage_survey_answer), onClick = onAnswer)
                PlainTextButton(
                    text = stringResource(R.string.notes_usage_survey_not_now),
                    onClick = onNotNow,
                    tone = TextButtonTone.Muted,
                )
            }
        }
    }
}

@Composable
private fun ThanksCard(onClosed: () -> Unit) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SurveyCardText(
                    title = stringResource(R.string.notes_usage_survey_thanks_title),
                    body = stringResource(R.string.notes_usage_survey_thanks_body),
                )
            }
            CountdownCloseButton(onClosed = onClosed)
        }
    }
}

@Composable
private fun SurveyCardText(title: String, body: String) {
    Text(
        text = title,
        style = OffhandTheme.type.rowTitle,
        color = OffhandTheme.colors.ink,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = body,
        style = OffhandTheme.type.bodySmall,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(top = 4.dp),
    )
}

// The ring runs down by the clock, not by an animation spec: the system's
// animator scale would otherwise shorten or skip the ten seconds.
@Composable
private fun CountdownCloseButton(onClosed: () -> Unit) {
    val close by rememberUpdatedState(onClosed)
    val isStill = isReducedMotion()
    var remaining by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(isStill) {
        if (isStill) {
            delay(THANKS_VISIBLE_MS)
        } else {
            val startedAt = withFrameMillis { it }
            while (remaining > 0f) {
                remaining = 1f - (withFrameMillis { it } - startedAt).toFloat() / THANKS_VISIBLE_MS
            }
        }
        close()
    }
    val description = stringResource(R.string.notes_usage_survey_close)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(OffhandSpacing.touchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = { close() }),
    ) {
        CountdownRing(remaining = remaining.coerceIn(0f, 1f))
        Icon(OffhandIcons.Close, contentDescription = description, tint = OffhandTheme.colors.ink2, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun CountdownRing(remaining: Float) {
    val track = OffhandTheme.colors.hairline
    val accent = OffhandTheme.colors.accent
    Canvas(modifier = Modifier.size(RING_SIZE)) {
        val stroke = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        drawArc(track, RING_START_ANGLE, FULL_CIRCLE, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
        if (remaining > 0f) {
            drawArc(accent, RING_START_ANGLE, FULL_CIRCLE * remaining, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
        }
    }
}

private const val THANKS_VISIBLE_MS = 10_000L
private const val RING_START_ANGLE = -90f
private const val FULL_CIRCLE = 360f
private val RING_SIZE = 32.dp
private val RING_STROKE = 2.5.dp
