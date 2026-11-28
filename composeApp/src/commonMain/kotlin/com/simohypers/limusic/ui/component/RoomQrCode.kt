package com.simohypers.limusic.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.simohypers.limusic.expect.ui.QrMatrix
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import kotlin.math.hypot
import kotlin.math.min

/**
 * A QR code drawn for people: modules rounded where they stand alone and joined where they touch,
 * the three eyes as rounded squares, the app's mark in the middle. It draws the modules only, with
 * a quiet zone of [QUIET_MODULES] inside its bounds; the light tile under it is the caller's.
 *
 * [reveal] (read while drawing, 0 to 1) builds it quietly: the modules grow in from the middle
 * outwards, the eyes and then the mark fade up, nothing overshoots.
 */
@Composable
fun RoomQrCode(
    matrix: QrMatrix,
    reveal: () -> Float,
    ink: Color,
    eyeInk: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val modules = remember(matrix) { modulesOf(matrix) }
    val logo = painterResource(Res.drawable.app_icon)
    Spacer(
        modifier
            .semantics { contentDescription = description }
            .drawWithCache {
                val n = matrix.size
                val side = min(size.width, size.height)
                val cell = side / (n + 2 * QUIET_MODULES)
                val origin = Offset((size.width - n * cell) / 2f, (size.height - n * cell) / 2f)
                val full = Path().also { p -> modules.forEach { p.addModule(it, origin, cell, 1f) } }
                val partial = Path()
                onDrawBehind {
                    val r = reveal().coerceIn(0f, 1f)
                    if (r <= 0f) return@onDrawBehind
                    if (r >= 1f) {
                        drawPath(full, ink)
                    } else {
                        partial.reset()
                        modules.forEach { m ->
                            val t = ((r - m.delay) / MODULE_SPAN).coerceIn(0f, 1f)
                            if (t > 0f) partial.addModule(m, origin, cell, easeOutCubic(t))
                        }
                        drawPath(partial, ink)
                    }
                    EYES.forEachIndexed { i, (ex, ey) ->
                        val t = ((r - EYE_START - i * EYE_STAGGER) / EYE_SPAN).coerceIn(0f, 1f)
                        if (t > 0f) drawEye(origin + Offset((if (ex) n - 7 else 0) * cell, (if (ey) n - 7 else 0) * cell), cell, eyeInk, t)
                    }
                    if (matrix.logoSpan > 0) {
                        val t = ((r - LOGO_START) / (1f - LOGO_START)).coerceIn(0f, 1f)
                        if (t > 0f) {
                            val span = matrix.logoSpan
                            val inner = (span - 1) * cell
                            val topLeft = origin + Offset(((n - span) / 2 + 0.5f) * cell, ((n - span) / 2 + 0.5f) * cell)
                            val s = 0.9f + 0.1f * easeOutCubic(t)
                            withTransform({ scale(s, s, topLeft + Offset(inner / 2f, inner / 2f)) }) {
                                translate(topLeft.x, topLeft.y) { with(logo) { draw(Size(inner, inner), alpha = t) } }
                            }
                        }
                    }
                }
            },
    )
}

/** Dark enough on white for any scanner, in the accent's hue: (modules, eyes). */
fun qrInks(accent: Color): Pair<Color, Color> {
    fun darken(
        c: Color,
        maxLuminance: Float,
    ): Color {
        var out = c
        while (out.luminance() > maxLuminance) out = lerp(out, Color.Black, 0.12f)
        return out
    }
    return darken(lerp(accent, Color(0xFF0E0F12), 0.6f), 0.035f) to darken(accent, 0.07f)
}

/** Light modules around the code, in modules. The spec asks for 4; scanners read 2 and up. */
const val QUIET_MODULES = 2.5f

private class Module(
    val x: Int,
    val y: Int,
    val topLeft: Boolean,
    val topRight: Boolean,
    val bottomRight: Boolean,
    val bottomLeft: Boolean,
    val delay: Float,
)

/** The dark data modules, each with the corners that round off and when it appears. */
private fun modulesOf(m: QrMatrix): List<Module> {
    val n = m.size
    fun dark(
        x: Int,
        y: Int,
    ) = m[x, y] && !m.inFinder(x, y) && !m.inLogo(x, y)
    val mid = (n - 1) / 2f
    val far = hypot(mid, mid)
    return buildList {
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (!dark(x, y)) continue
                val left = dark(x - 1, y)
                val right = dark(x + 1, y)
                val up = dark(x, y - 1)
                val down = dark(x, y + 1)
                add(
                    Module(
                        x, y,
                        topLeft = !left && !up,
                        topRight = !right && !up,
                        bottomRight = !right && !down,
                        bottomLeft = !left && !down,
                        delay = hypot(x - mid, y - mid) / far * MODULE_SPREAD,
                    ),
                )
            }
        }
    }
}

private fun Path.addModule(
    m: Module,
    origin: Offset,
    cell: Float,
    scale: Float,
) {
    val half = cell / 2f * scale
    val cx = origin.x + (m.x + 0.5f) * cell
    val cy = origin.y + (m.y + 0.5f) * cell
    val round = CornerRadius(half)
    fun corner(rounded: Boolean) = if (rounded) round else CornerRadius.Zero
    addRoundRect(
        RoundRect(
            left = cx - half,
            top = cy - half,
            right = cx + half,
            bottom = cy + half,
            topLeftCornerRadius = corner(m.topLeft),
            topRightCornerRadius = corner(m.topRight),
            bottomRightCornerRadius = corner(m.bottomRight),
            bottomLeftCornerRadius = corner(m.bottomLeft),
        ),
    )
}

/** One 7×7 eye at [topLeft]: a rounded ring and a rounded 3×3 core, fading up as [t] runs to 1. */
private fun DrawScope.drawEye(
    topLeft: Offset,
    cell: Float,
    color: Color,
    t: Float,
) {
    val box = 7 * cell
    val pivot = topLeft + Offset(box / 2f, box / 2f)
    val s = 0.85f + 0.15f * easeOutCubic(t)
    withTransform({ scale(s, s, pivot) }) {
        drawRoundRect(
            color = color,
            topLeft = topLeft + Offset(cell / 2f, cell / 2f),
            size = Size(box - cell, box - cell),
            cornerRadius = CornerRadius(cell * 1.9f),
            style = Stroke(width = cell),
            alpha = t,
        )
        drawRoundRect(
            color = color,
            topLeft = topLeft + Offset(2 * cell, 2 * cell),
            size = Size(3 * cell, 3 * cell),
            cornerRadius = CornerRadius(cell * 0.95f),
            alpha = t,
        )
    }
}

private fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

// The reveal's timeline, as fractions of it.
private const val MODULE_SPREAD = 0.35f // the outermost module starts here
private const val MODULE_SPAN = 0.5f // how long one module takes to grow
private const val EYE_START = 0.3f
private const val EYE_STAGGER = 0.04f
private const val EYE_SPAN = 0.5f
private const val LOGO_START = 0.55f

// Top left, top right, bottom left: (right side, bottom side).
private val EYES = listOf(false to false, true to false, false to true)
