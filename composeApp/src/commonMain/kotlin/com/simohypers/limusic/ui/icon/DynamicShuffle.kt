package com.simohypers.limusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.simohypers.limusic.domain.mediaservice.handler.ControlState

/** A four-point sparkle centred on 4.8,4.8 with its tips 3.9 out, the sides drawn in toward the middle. */
private const val SPARKLE = "M4.8,0.9 C5,3.24 6.36,4.6 8.7,4.8 C6.36,5 5,6.36 4.8,8.7 C4.6,6.36 3.24,5 0.9,4.8 C3.24,4.6 4.6,3.24 4.8,0.9 Z"

/**
 * Dynamic shuffle's mark: [Shuffle] (Material Symbols Rounded, Apache 2.0) with its top-left stroke
 * cut back to a stub and a sparkle where it started, the same sparkle that marks a suggested row
 * ([Suggested]). The two arrows are Shuffle's own paths.
 */
val LimusicIcons.DynamicShuffle: ImageVector
    get() =
        _DynamicShuffle ?: ImageVector
            .Builder(
                name = "DynamicShuffle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).addPath(
                pathData =
                    addPathNodes(
                        "M15,20 q-0.42,0 -0.71,-0.29 Q14,19.43 14,19 t0.29,-0.71 T15,18 h1.6 L14.13,15.53 q-0.3,-0.3 -0.29,-0.71 " +
                            "T14.15,14.1 t0.71,-0.3 t0.71,0.3 L18,16.55 V15 q0,-0.43 0.29,-0.71 T19,14 t0.71,0.29 T20,15 v4 " +
                            "q0,0.43 -0.29,0.71 T19,20 H15 Z" +
                            "M4.3,19.7 Q4.03,19.43 4.03,19 T4.3,18.3 L16.6,6 H15 Q14.58,6 14.29,5.71 Q14,5.43 14,5 T14.29,4.29 " +
                            "T15,4 h4 q0.43,0 0.71,0.29 T20,5 V9 q0,0.42 -0.29,0.71 T19,10 T18.29,9.71 T18,9 V7.4 L5.7,19.7 " +
                            "Q5.43,19.98 5,19.98 q-0.42,0 -0.7,-0.28 Z" +
                            // The top-left stroke, from a little past the sparkle to where Shuffle's ends.
                            "M8.01,6.59 L9.89,8.47 A1,1 0 0 1 8.47,9.89 L6.59,8.01 A1,1 0 0 1 8.01,6.59 Z" +
                            SPARKLE,
                    ),
                fill = SolidColor(Color.Black),
            ).build()
            .also { _DynamicShuffle = it }

private var _DynamicShuffle: ImageVector? = null

/** The sparkle on its own, filling the box: what a track dynamic shuffle lined up is marked with. */
val LimusicIcons.Suggested: ImageVector
    get() =
        _Suggested ?: ImageVector
            .Builder(
                name = "Suggested",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).addPath(
                pathData = addPathNodes("M12,2 C12.5,8 16,11.5 22,12 C16,12.5 12.5,16 12,22 C11.5,16 8,12.5 2,12 C8,11.5 11.5,8 12,2 Z"),
                fill = SolidColor(Color.Black),
            ).build()
            .also { _Suggested = it }

private var _Suggested: ImageVector? = null

/** The player's shuffle button glyph for this state. */
val ControlState.shuffleIcon: ImageVector get() = if (isDynamicShuffle) LimusicIcons.DynamicShuffle else LimusicIcons.Shuffle
