package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmytrosamoilov.offhand.core.designsystem.R

// The gating marker: a "PRO" pill placed right after a feature's title. The
// crown stays for the tier itself (paywall, Settings card).
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Surface(color = ProGold.copy(alpha = BADGE_TINT_ALPHA), shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(
            text = stringResource(R.string.designsystem_pro_badge_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = ProGold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private const val BADGE_TINT_ALPHA = 0.18f
