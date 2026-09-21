package com.dmytrosamoilov.offhand.feature.paywall.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import org.koin.compose.koinInject

// Sits above the whole app: any ViewModel that calls ProUpgradeGate.requirePro()
// makes this overlay appear, and closing it resumes that call.
@Composable
fun PaywallHost() {
    val gate: ProUpgradeGate = koinInject()
    val requestedFeature by gate.requestedFeature.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = requestedFeature != null,
        enter = slideInVertically(initialOffsetY = { it / 4 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(),
    ) {
        PaywallScreen()
    }
}
