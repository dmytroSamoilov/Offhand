package com.dmytrosamoilov.offhand.root

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.dmytrosamoilov.offhand.R
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandDarkContent
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandSpacing
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun LockScreen(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)

    LaunchedEffect(Unit) {
        context.showUnlockPrompt { currentOnAuthenticated() }
    }

    OffhandDarkContent {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(OffhandTheme.colors.ground)
                .safeDrawingPadding()
                .padding(start = OffhandSpacing.screen, end = OffhandSpacing.screen, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LockMessage(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.lock_unlock_button),
                onClick = { context.showUnlockPrompt { currentOnAuthenticated() } },
                icon = OffhandIcons.Unlock,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LockMessage(modifier: Modifier = Modifier) {
    val colors = OffhandTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(1.dp, colors.border, CircleShape),
        ) {
            Icon(imageVector = OffhandIcons.Lock, contentDescription = null, tint = colors.ink, modifier = Modifier.size(28.dp))
        }
        Text(
            text = stringResource(R.string.lock_title),
            style = OffhandTheme.type.title,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = stringResource(R.string.lock_body),
            style = OffhandTheme.type.dialogBody,
            color = colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}

private fun Context.showUnlockPrompt(onSuccess: () -> Unit) {
    val activity = findFragmentActivity() ?: return
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.lock_prompt_title))
        .setSubtitle(activity.getString(R.string.lock_prompt_subtitle))
        .setAllowedAuthenticators(
            Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
        )
        .build()
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(promptInfo)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
