package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.R

// The gold crown that marks a Pro-only action for a free user; the same art
// ships as ProCrown.imageset on iOS. Gold is a brand mark, so it is not tinted
// by the theme.
@Composable
fun ProCrown(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_pro_crown),
        contentDescription = stringResource(R.string.designsystem_pro_badge),
        modifier = modifier.size(size),
    )
}
