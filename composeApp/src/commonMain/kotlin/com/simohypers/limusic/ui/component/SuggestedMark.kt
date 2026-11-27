package com.simohypers.limusic.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Suggested
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.suggested_by_dynamic_shuffle
import org.jetbrains.compose.resources.stringResource

/**
 * A player's title with dynamic shuffle's sparkle right after it while [suggested]: the title takes
 * the room it needs and the sparkle follows it, at the end when the title is long. [title] gets the
 * Row's weight, so it must not fill the width itself.
 */
@Composable
fun TitleWithSuggestedMark(
    suggested: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    title: @Composable RowScope.(Modifier) -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        title(Modifier.weight(1f, fill = false))
        if (suggested) {
            Icon(
                imageVector = LimusicIcons.Suggested,
                contentDescription = stringResource(Res.string.suggested_by_dynamic_shuffle),
                tint = tint,
                modifier = Modifier.padding(start = 6.dp).size(size),
            )
        }
    }
}
