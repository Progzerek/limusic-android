package com.simohypers.limusic.ui.screen.player.content.immersive

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.domain.mediaservice.handler.ControlState
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState
import com.simohypers.limusic.domain.repository.ListenTogetherRepository
import com.simohypers.limusic.Platform
import com.simohypers.limusic.expect.ui.DeviceVolumeController
import com.simohypers.limusic.extension.formatDuration
import com.simohypers.limusic.getPlatform
import com.simohypers.limusic.ui.component.ExplicitBadge
import com.simohypers.limusic.ui.component.heartBurst
import com.simohypers.limusic.ui.component.rememberHeartBurstState
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.component.TitleWithSuggestedMark
import com.simohypers.limusic.ui.icon.AddCircleOutline
import com.simohypers.limusic.ui.icon.CheckCircle
import com.simohypers.limusic.ui.icon.FastForward
import com.simohypers.limusic.ui.icon.FastRewind
import com.simohypers.limusic.ui.icon.Groups
import com.simohypers.limusic.ui.icon.Headphones
import com.simohypers.limusic.ui.icon.Lyrics
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.Pause
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.QueueMusic
import com.simohypers.limusic.ui.icon.Repeat
import com.simohypers.limusic.ui.icon.RepeatOne
import com.simohypers.limusic.ui.icon.shuffleIcon
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.FavoriteBorder
import com.simohypers.limusic.ui.icon.VolumeDown
import com.simohypers.limusic.ui.icon.VolumeUp
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentActions
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentState
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.crossfading
import limusic.composeapp.generated.resources.live_badge
import kotlin.math.roundToLong
import org.koin.compose.koinInject

/** Which body the dock is currently showing. Held by the top-level Immersive composable. */
internal enum class ImmersiveView { MAIN, LYRICS, QUEUE }

/**
 * The page gradient's colour at [fraction] of the screen height: the seed barely darkened at the
 * top, ~32%-darkened by mid-page (48%), ~78%-darkened at the bottom. Used to BUILD the gradient
 * and to land the artwork fade on the gradient's own colour at the exact Y where it ends.
 */
internal fun immersiveGradientColorAt(
    seedColor: Color,
    fraction: Float,
): Color {
    // Every stop is the SEED darkened, never a fixed near-black: the old bottom stop was a
    // hardcoded warm black, so on the artwork view — where only the lower half of the gradient
    // is visible under the artwork — the page read as plain black instead of tinted.
    val top = lerp(seedColor, Color.Black, 0.05f)
    val mid = lerp(seedColor, Color.Black, 0.32f)
    val bottom = lerp(seedColor, Color.Black, 0.78f)
    return if (fraction <= 0.48f) {
        lerp(top, mid, (fraction / 0.48f).coerceIn(0f, 1f))
    } else {
        lerp(mid, bottom, ((fraction - 0.48f) / 0.52f).coerceIn(0f, 1f))
    }
}

internal val ImmersiveTextSecondary = Color.White.copy(alpha = 0.72f)

// Artist, times and captions.
internal val ImmersiveSecondaryText = Color.White.copy(alpha = 0.55f)
internal val ImmersivePillInactive = Color.White.copy(alpha = 0.24f)
internal val ImmersiveTrackInactive = Color.White.copy(alpha = 0.26f)
internal val ImmersiveTrackActive = Color.White.copy(alpha = 0.92f)

@Immutable
internal data class ImmersiveTypography(
    val mainTitle: TextStyle,
    val mainArtist: TextStyle,
    val compactTitle: TextStyle,
    val compactArtist: TextStyle,
    val queueSectionHeader: TextStyle,
    val queueSectionSubtitle: TextStyle,
    val times: TextStyle,
    val footer: TextStyle,
    val idleLyric: TextStyle,
    val idleTranslated: TextStyle,
    val lyricStrip: TextStyle,
)

/**
 * Maps every Immersive text slot 1:1 onto the [typo] roles the OTHER styles use for the same
 * element — no custom sizes, no custom scaling (owner's rule: this style types like the rest of
 * the app). Precedents: M3E's track title/artist row (titleMedium/bodyMedium), LyricsView's
 * in-player line (headlineMedium), SongFullWidthItems rows (titleSmall/bodySmall), the queue
 * sheet's section headers (titleMedium) and M3E's canvas overlay lines (bodyMedium white/yellow).
 *
 * Secondary text is a translucent white rather than typo()'s body grey: an opaque
 * #A8A8A8 is darker than a bright sleeve's page and read as a dark grey smudge on it, where a
 * translucent white stays lighter than whatever it sits on.
 */
@Composable
internal fun rememberImmersiveTypography(): ImmersiveTypography {
    val t = typo()
    return ImmersiveTypography(
        mainTitle = t.titleMedium,
        mainArtist = t.bodyMedium.copy(color = ImmersiveSecondaryText),
        // The compact header — Lyrics, Queue, and the row under a running canvas — carries the
        // SIZES FullscreenLyricsSheet uses for the same job (LyricsView.kt: labelSmall over
        // bodySmall), while mainTitle/mainArtist stay 18/13 because they head the controller
        // layout. Borrow the size from labelSmall but keep the titleMedium ROLE: typo() bakes a
        // color into every role — title* carry titleColor, body*/label* carry bodyColor — so
        // switching the title to labelSmall outright would also switch it to the subtitle's grey.
        compactTitle = t.titleMedium.copy(fontSize = t.labelSmall.fontSize),
        compactArtist = t.bodySmall.copy(color = ImmersiveSecondaryText),
        queueSectionHeader = t.titleMedium,
        queueSectionSubtitle = t.bodySmall.copy(color = ImmersiveSecondaryText),
        times = t.bodyMedium.copy(color = ImmersiveSecondaryText),
        footer = t.bodySmall.copy(color = ImmersiveSecondaryText),
        idleLyric = t.bodyMedium.copy(color = Color.White),
        idleTranslated = t.bodyMedium.copy(color = Color.Yellow),
        // The line over the progress bar is the inline lyric line Classic and M3E draw under their
        // artwork: labelSmall in white, the same role and colour.
        lyricStrip = t.labelSmall.copy(color = Color.White),
    )
}

/**
 * True alpha fade at the top/bottom edges of a scrolling region (DstIn mask): content dissolves
 * into whatever is behind it — the missing "scrim" on the lyrics list's hard-clipped edges —
 * without painting a color and without touching the wrapped component.
 *
 * Both ramps follow smoothstep, the curve [com.simohypers.limusic.extension.smoothScrimBrush] uses and
 * for the reason it gives: a linear ramp has a corner where it leaves 0, and the eye reads that corner
 * as an edge — on the artwork it was a visible line where the picture started to dissolve.
 */
internal fun Modifier.immersiveVerticalFadeEdges(
    topFade: Dp,
    bottomFade: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = topFade.toPx().coerceAtMost(size.height / 2f)
            val bottomPx = bottomFade.toPx().coerceAtMost(size.height / 2f)
            val topStop = if (size.height > 0f) topPx / size.height else 0f
            val bottomStop = if (size.height > 0f) 1f - bottomPx / size.height else 1f
            drawRect(
                brush = Brush.verticalGradient(colorStops = fadeEdgeStops(topStop, bottomStop)),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * Transparent at [transparentAt] and beyond it, opaque at [opaqueAt] and beyond it, on the same
 * smoothstep curve as [immersiveVerticalFadeEdges] — a fade placed anywhere in the element rather
 * than at its edge, in either direction.
 */
internal fun Modifier.immersiveFadeBetween(
    transparentAt: Dp,
    opaqueAt: Dp,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colorStops = fadeEdgeStops(1f, 1f),
                        startY = transparentAt.toPx(),
                        endY = opaqueAt.toPx(),
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

private const val FADE_EDGE_STEPS = 24

private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

// Transparent → opaque over [0, topStop], held, then opaque → transparent over [bottomStop, 1]. A
// zero-length ramp is skipped rather than drawn as a pile of stops at one position.
private fun fadeEdgeStops(
    topStop: Float,
    bottomStop: Float,
): Array<Pair<Float, Color>> =
    buildList {
        if (topStop > 0f) {
            for (i in 0..FADE_EDGE_STEPS) {
                val t = i / FADE_EDGE_STEPS.toFloat()
                add(topStop * t to Color.Black.copy(alpha = smoothstep(t)))
            }
        } else {
            add(0f to Color.Black)
        }
        if (bottomStop < 1f) {
            for (i in 0..FADE_EDGE_STEPS) {
                val t = i / FADE_EDGE_STEPS.toFloat()
                add(bottomStop + (1f - bottomStop) * t to Color.Black.copy(alpha = 1f - smoothstep(t)))
            }
        } else {
            add(1f to Color.Black)
        }
    }.toTypedArray()

/**
 * Press-to-swell ("phồng to ra") like the liquid-glass buttons: the control springs up while a
 * finger is on it and settles back on release.
 *
 * Detection is a plain awaitEachGesture down/up watch, NOT GlassInteraction's drag inspector —
 * that one only starts its animation from a DRAG start, so a normal tap on these buttons never
 * animated anything. `requireUnconsumed = false` keeps it working under the wrapped `clickable`,
 * and nothing here consumes events, so clicks and slider drags still land.
 */
@Composable
internal fun Modifier.immersivePressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "immersivePressInflate",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * One 25dp glyph button with a tight circular ripple — [IconButton] sized and clipped exactly
 * like Classic/M3E's plain icon buttons (Info, PlaylistAdd, Queue, Replay5…), not a custom
 * Box+clickable and not an unconstrained [com.simohypers.limusic.ui.component.RippleIconButton].
 */
@Composable
internal fun ImmersiveGlyphButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.immersivePressInflate().size(size).clip(CircleShape),
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = tint)
    }
}

/**
 * The ⊕ (YouTube-liked) / ♥ (favourite, with the heart-burst) / ⋯ (more sheet) trio — shared by
 * the MAIN title row and the LYRICS/QUEUE compact header. Fires the heart-burst on the tap that
 * likes, never on state, matching every other style's like button.
 */
@Composable
internal fun ImmersiveHeaderActions(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val likeBurst = rememberHeartBurstState()
        Box(
            modifier =
                Modifier
                    .immersivePressInflate()
                    .size(32.dp)
                    .heartBurst(likeBurst)
                    .clip(CircleShape)
                    .clickable {
                        if (!state.controllerState.isLiked) likeBurst.fire()
                        actions.onUIEvent(UIEvent.ToggleLike)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = state.controllerState.isLiked, label = "immersiveFavorite") { liked ->
                Icon(
                    imageVector = if (liked) LimusicIcons.Favorite else LimusicIcons.FavoriteBorder,
                    contentDescription = "",
                    tint = Color.White,
                    // The ⋯ glyph's size; the 32dp box around it keeps the tap target.
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        ImmersiveGlyphButton(icon = LimusicIcons.MoreVert, onClick = { actions.onShowMoreSheet() })
    }
}

/**
 * 56dp rounded thumbnail + title/artist column + [ImmersiveHeaderActions], used where the
 * artwork isn't on screen (LYRICS and QUEUE bodies).
 */
@Composable
internal fun ImmersiveCompactHeader(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: ImmersiveTypography,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(state.screenData.thumbnailURL)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(state.screenData.thumbnailURL)
                    .crossfade(300)
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = null,
            modifier = Modifier.size(55.dp).clip(rounded(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Ellipsis, not marquee: a marquee in this narrow header scrolls constantly and
            // snapshots as garbage ("Vill Be Okay … Eve" in the first device screenshots).
            TitleWithSuggestedMark(state.suggested, Modifier.fillMaxWidth(), size = 12.dp, tint = typography.compactTitle.color) { weight ->
                Text(
                    text = state.screenData.nowPlayingTitle,
                    style = typography.compactTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = weight,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.screenData.isExplicit) {
                    ExplicitBadge(modifier = Modifier.size(20.dp).padding(end = 4.dp))
                }
                Text(
                    text = state.screenData.artistName,
                    style = typography.compactArtist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { actions.onNavigateToArtist() },
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        ImmersiveHeaderActions(state = state, actions = actions)
    }
}

/** 8.4dp track shared by the progress bar and the volume row — same visual language, only the color and callbacks differ. */
@Composable
internal fun ImmersiveThinSlider(
    value: Float,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    // The slider's "phồng" is a track that thickens while touched/dragged — same spring feel as
    // the button inflate, expressed the way a bar can.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    // 20% thicker than the original 7dp/14dp at the owner's request, so the bar is easier to hit.
    // The swollen height still fits the 18dp shells both callers wrap it in.
    val trackHeight by animateDpAsState(
        targetValue = if (pressed || dragged) 16.8.dp else 8.4.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "immersiveSliderInflate",
    )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            interactionSource = interactionSource,
            track = {
                // Hand-drawn instead of SliderDefaults.Track. M3 rounds each half of the track with
                // TWO different radii: the OUTER end with trackCornerSize (height/2 — fully round)
                // and the INNER end, where the two halves meet, with trackInsideCornerSize (2dp —
                // reads as square). At value 0 the inactive half owns the entire bar, so its square
                // inner end lands on the LEFT while its round outer end sits on the right — the
                // mismatched bar. Clipping ONE container and drawing the played portion inside it
                // keeps both ends of the bar equally round, and gives the played portion a flat
                // right edge.
                val fraction = value.coerceIn(0f, 1f)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(ImmersiveTrackInactive),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(activeColor),
                    )
                }
            },
            thumb = {
                // No thumb — the approved mock is track-only; the
                // active/inactive split marks the position (same call the MiniPlayer makes).
                Spacer(Modifier.size(0.dp))
            },
        )
    }
}

/** Elapsed time, the quality line (hidden when unknown), and the remaining time as "-m:ss". */
@Composable
internal fun ImmersiveTimesRow(
    state: NowPlayingContentState,
    typography: ImmersiveTypography,
    modifier: Modifier = Modifier,
) {
    // `total` stays -1 until the player reports a duration, and SimpleMediaState.Ready carries
    // ONLY the duration — never a position. After a queue restore nothing is playing, and the
    // position poll only runs while isPlaying, so no later event arrives to correct it. Deriving
    // elapsed from total therefore zeroed BOTH numbers at once, which is why a restored queue read
    // 00:00 / -00:00: the played time was never actually unknown, TimeLine.current held it.
    val knownTotal = state.timelineState.total.takeIf { it > 0L }
    val elapsedMs =
        if (knownTotal != null) {
            // Still derived from the slider while the duration IS known, so this number tracks the
            // finger while scrubbing instead of waiting for the player to report the seek back.
            (knownTotal * (state.sliderValue / 100f)).roundToLong()
        } else {
            state.timelineState.current.coerceAtLeast(0L)
        }
    // Clamp BEFORE formatDuration — it renders any negative as "NA:NA", and the remaining time
    // must never show that at the end of a track whose length IS known. An UNKNOWN length is
    // exactly what that string is for, so null deliberately takes the negative path below.
    val remainingMs = knownTotal?.let { (it - elapsedMs).coerceAtLeast(0L) }
    // A Box, not a three-way weighted Row: the quality line ("154 kbps · 48 kHz") is far wider than
    // the codec pill it replaced, and a third of the row wrapped it onto two lines. Pinned to the
    // row's own centre it has everything between the two times, and a minute rolling over on
    // either side cannot nudge it.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                // Blank for a live broadcast: where it sits in the broadcast's seek window means nothing.
                text = if (state.timelineState.isLive) "" else formatDuration(elapsedMs),
                style = typography.times,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Left,
            )
            Text(
                // No leading "-" when the length is unknown: "-NA:NA" reads as a negative amount of
                // nothing. formatDuration's own out-of-range string is the app's established way to
                // say "no value here".
                text =
                    if (state.timelineState.isLive) {
                        stringResource(Res.string.live_badge)
                    } else {
                        remainingMs?.let { "-" + formatDuration(it) } ?: formatDuration(-1L)
                    },
                style = typography.times,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Right,
            )
        }
        // ONE slot holding two alternatives — and both of them stay composed, swapping on alpha
        // rather than on presence. AnimatedVisibility removes its content from the LAYOUT, so the
        // slot would take the height of whichever state was up, and with NEITHER showing (no
        // quality known, not crossfading) it would collapse to nothing — every swap resizing this
        // row and shoving the transport below it. Holding both keeps the height fixed. The
        // cross-fade looks identical — alpha is what fadeIn/fadeOut animated anyway.
        Box(contentAlignment = Alignment.Center) {
            // Sweep head for the "Crossfading" shimmer, 0..1. Runs UNCONDITIONALLY — put behind the
            // crossfade check it would restart from zero every time the label appears, which is the
            // same reason the other two styles declare it outside their own visibility gate.
            val sweepTransition = rememberInfiniteTransition(label = "immersiveCrossfadeSweep")
            val crossfadeSweep by sweepTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(3200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "immersiveSweepHead",
            )
            val quality = state.audioQualityLabel
            val crossfadeLabelAlpha by animateFloatAsState(
                targetValue = if (state.timelineState.isCrossfading) 1f else 0f,
                label = "immersiveCrossfadeLabelAlpha",
            )
            val qualityLabelAlpha by animateFloatAsState(
                targetValue = if (!state.timelineState.isCrossfading && quality != null) 1f else 0f,
                label = "immersiveQualityLabelAlpha",
            )
            Box(modifier = Modifier.alpha(crossfadeLabelAlpha)) {
                // Identical treatment to Classic and M3 Expressive: a highlight swept through the
                // glyphs with a text brush — no overlay, no clipping.
                val shimmerSpan = 140f
                val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                val labelColor = typography.times.color
                Text(
                    text = stringResource(Res.string.crossfading),
                    style =
                        typography.times.copy(
                            brush =
                                Brush.horizontalGradient(
                                    0f to labelColor.copy(alpha = 0.45f),
                                    // The sweep head is PURE white, not the resting label colour — that
                                    // colour is an adaptive grey, and a grey gleam reads as no gleam.
                                    0.5f to Color.White,
                                    1f to labelColor.copy(alpha = 0.45f),
                                    startX = shimmerHead,
                                    endX = shimmerHead + shimmerSpan,
                                    tileMode = TileMode.Clamp,
                                ),
                        ),
                    textAlign = TextAlign.Center,
                )
            }
            // The quality line: a small headphones glyph ahead of the stream's own figures,
            // bare on the page and dimmer than the times either side of it. The figures change
            // with the track and so crossfade in with it rather than snapping.
            Crossfade(
                targetState = quality,
                animationSpec = tween(300),
                label = "immersiveQualityLabel",
                modifier = Modifier.alpha(qualityLabelAlpha),
            ) { label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = LimusicIcons.Headphones,
                        contentDescription = null,
                        tint = ImmersiveQualityLabel,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // orEmpty(), not label!!: the null check drives the alpha above rather than
                    // guarding this branch. It also renders while alpha is 0, which keeps the slot
                    // its height.
                    Text(
                        text = label.orEmpty(),
                        style = typography.times.copy(color = ImmersiveQualityLabel),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// The quality line sits a step below the times either side of it.
private val ImmersiveQualityLabel = Color.White.copy(alpha = 0.45f)

/** FastRewind(44dp) → Previous, Play/Pause(62dp, plain white — no container disc), FastForward(44dp) → Next. */
@Composable
internal fun ImmersiveTransportRow(
    controllerState: ControlState,
    onUIEvent: (UIEvent) -> Unit,
    modifier: Modifier = Modifier,
    // Only the fullscreen lyrics page asks for these; the Now Playing page keeps its three buttons.
    showShuffleAndRepeat: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Mock: a tight centered cluster with a 58dp gap — NOT SpaceEvenly, which spreads the
        // rewind/forward glyphs to the screen edges (first device screenshots). With shuffle and
        // repeat on the two ends the five span the row instead: five buttons at a 58dp gap no longer fit beside the artwork.
        horizontalArrangement =
            if (showShuffleAndRepeat) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showShuffleAndRepeat) {
            ImmersiveGlyphButton(
                icon = controllerState.shuffleIcon,
                onClick = { onUIEvent(UIEvent.Shuffle) },
                size = 40.dp,
                tint = Color.White.copy(alpha = if (controllerState.isShuffle) 1f else 0.4f),
            )
        }
        IconButton(
            onClick = { if (controllerState.isPreviousAvailable) onUIEvent(UIEvent.Previous) },
            modifier = Modifier.immersivePressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = LimusicIcons.FastRewind,
                contentDescription = "",
                tint = Color.White.copy(alpha = if (controllerState.isPreviousAvailable) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
        // No loading state on this button. It is play/pause and nothing else: it stays pressable
        // and keeps showing the transport glyph even while the player is buffering, so the control
        // never disappears out from under a finger reaching for it.
        Box(
            modifier =
                Modifier
                    .immersivePressInflate()
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable { onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.isPlaying, label = "immersivePlayPauseIcon") { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) LimusicIcons.Pause else LimusicIcons.PlayArrow,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.size(66.dp),
                )
            }
        }
        IconButton(
            onClick = { if (controllerState.isNextAvailable) onUIEvent(UIEvent.Next) },
            modifier = Modifier.immersivePressInflate().size(56.dp).clip(CircleShape),
        ) {
            Icon(
                imageVector = LimusicIcons.FastForward,
                contentDescription = "",
                tint = Color.White.copy(alpha = if (controllerState.isNextAvailable) 1f else 0.4f),
                modifier = Modifier.size(46.dp),
            )
        }
        if (showShuffleAndRepeat) {
            val repeatState = controllerState.repeatState
            ImmersiveGlyphButton(
                icon = if (repeatState is RepeatState.One) LimusicIcons.RepeatOne else LimusicIcons.Repeat,
                onClick = { onUIEvent(UIEvent.Repeat) },
                size = 40.dp,
                tint = Color.White.copy(alpha = if (repeatState !is RepeatState.None) 1f else 0.4f),
            )
        }
    }
}

@Composable
internal fun ImmersiveVolumeRow(
    controller: DeviceVolumeController,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = LimusicIcons.VolumeDown,
            contentDescription = "",
            tint = ImmersiveTextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(18.dp), contentAlignment = Alignment.Center) {
            ImmersiveThinSlider(
                value = controller.volumeFraction,
                activeColor = ImmersiveTrackActive,
                onValueChange = { controller.setVolumeFraction(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = LimusicIcons.VolumeUp,
            contentDescription = "",
            tint = ImmersiveTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One 44dp dock button: a light circle behind a DARK glyph while [active] (white-on-light was unreadable). */
@Composable
internal fun ImmersiveDockButton(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .immersivePressInflate()
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color.Transparent)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            tint =
                when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    active -> activeContentColor
                    else -> Color.White.copy(alpha = 0.85f)
                },
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Lyrics · [Output | Listen Together] · Queue. The capsule holds the two
 * answers to "where is this playing": which speaker (the output sheet, which is also where Cast
 * lives now) and which people (Listen Together). Joined by a hairline rather than a gap, so the
 * two read as one object.
 */
@Composable
internal fun ImmersiveActionRow(
    viewState: ImmersiveView,
    onSelectView: (ImmersiveView) -> Unit,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onOpenOutput: () -> Unit,
    onOpenListenTogether: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Straight off the repository, like ListenTogetherIconButton: one boolean off a StateFlow is all
    // the capsule needs, and a ViewModel for it would be built every time the player opens.
    val listenTogether = koinInject<ListenTogetherRepository>()
    val room by listenTogether.room.collectAsStateWithLifecycle()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A quarter of the spare width at each end, the rest between. SpaceEvenly
        // over the full row pushed the two glyphs out to the gutters, away from the capsule.
        val content = DOCK_BUTTON_SIZE * 2 + CAPSULE_SEGMENT_WIDTH * 2 + 1.dp
        val edgeInset = ((maxWidth - content) / 4).coerceAtLeast(0.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = edgeInset),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Re-tapping the active tab returns to MAIN — the dock is a toggle, not one-way nav.
            ImmersiveDockButton(
                icon = LimusicIcons.Lyrics,
                active = viewState == ImmersiveView.LYRICS,
                activeColor = activeColor,
                activeContentColor = activeContentColor,
                enabled = lyricsAvailable,
                onClick = {
                    onSelectView(if (viewState == ImmersiveView.LYRICS) ImmersiveView.MAIN else ImmersiveView.LYRICS)
                },
            )
            Row(
                modifier =
                    Modifier
                        .height(DOCK_BUTTON_SIZE)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImmersiveCapsuleSegment(
                    icon = LimusicIcons.Headphones,
                    // Headphones is a tall, narrow glyph and Groups a wide one; these are the sizes at
                    // which the two read as a matched pair rather than one looking bigger.
                    iconSize = 23.dp,
                    highlighted = false,
                    onClick = onOpenOutput,
                )
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.20f)),
                )
                ImmersiveCapsuleSegment(
                    icon = LimusicIcons.Groups,
                    iconSize = 24.dp,
                    highlighted = room.inRoom,
                    onClick = onOpenListenTogether,
                )
            }
            ImmersiveDockButton(
                icon = LimusicIcons.QueueMusic,
                active = viewState == ImmersiveView.QUEUE,
                activeColor = activeColor,
                activeContentColor = activeContentColor,
                onClick = {
                    onSelectView(if (viewState == ImmersiveView.QUEUE) ImmersiveView.MAIN else ImmersiveView.QUEUE)
                },
            )
        }
    }
}

/**
 * One half of the capsule. Its highlight fills the segment edge to edge — a circle sized to the
 * glyph, as the dock buttons use, would stop the join reading as one object. The glyph swells on
 * press like every other control in this style; the segment itself cannot, or it would spill past
 * the capsule's rounded ends.
 */
@Composable
private fun ImmersiveCapsuleSegment(
    icon: ImageVector,
    iconSize: Dp,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val glyphScale by animateFloatAsState(
        targetValue = if (pressed) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "immersiveCapsuleGlyph",
    )
    val background by animateColorAsState(
        targetValue = if (highlighted) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(220),
        label = "immersiveCapsuleHighlight",
    )
    Box(
        modifier =
            Modifier
                .width(CAPSULE_SEGMENT_WIDTH)
                .fillMaxHeight()
                .background(background)
                .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) Color.White else Color.White.copy(alpha = 0.85f),
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = glyphScale
                        scaleY = glyphScale
                    }.size(iconSize),
        )
    }
}

private val DOCK_BUTTON_SIZE = 40.dp
private val CAPSULE_SEGMENT_WIDTH = 64.dp

/**
 * Progress bar + times + transport — the playback half of [ImmersiveBottomCluster]. Emits
 * straight into the caller's Column; the horizontal
 * gutter belongs to that Column, as it does in the cluster.
 */
@Composable
internal fun ColumnScope.ImmersivePlaybackControls(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: ImmersiveTypography,
    showShuffleAndRepeat: Boolean = false,
) {
    // Fixed 18dp shell: the track swells on touch, but inside a CONSTANT footprint —
    // otherwise the growing slider re-measures this whole column and the artwork above
    // it visibly jumps. It also gives the bar a real 18dp touch target instead of 8.4dp.
    Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
        ImmersiveThinSlider(
            value = state.sliderValue / 100f,
            activeColor = if (state.timelineState.isCrossfading) state.sliderTrackColor else ImmersiveTrackActive,
            onValueChange = { actions.onSliderChange(it * 100f) },
            onValueChangeFinished = actions.onSliderChangeFinished,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    ImmersiveTimesRow(state = state, typography = typography, modifier = Modifier.padding(top = 8.dp))
    Spacer(modifier = Modifier.height(if (state.isShort) 14.dp else 22.dp))
    ImmersiveTransportRow(
        controllerState = state.controllerState,
        onUIEvent = actions.onUIEvent,
        showShuffleAndRepeat = showShuffleAndRepeat,
    )
}

/**
 * Progress bar + times + transport + volume + action row + output caption — the fixed block every
 * Immersive body (MAIN, LYRICS, QUEUE) renders at the bottom, identically. On Desktop only the
 * action row renders (no slider/transport/volume), matching the `Platform.Android` gate the other
 * two styles use.
 *
 * [outputName] names the device the sound leaves by ("Simo's Phone", a Bluetooth headset, a
 * Cast receiver); the slot keeps its height while it is unknown, so nothing above it moves.
 */
@Composable
internal fun ImmersiveBottomCluster(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: ImmersiveTypography,
    viewState: ImmersiveView,
    onSelectView: (ImmersiveView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    deviceVolumeController: DeviceVolumeController?,
    outputName: String?,
    onOpenOutput: () -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 8.dp,
) {
    val localDensity = LocalDensity.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = topPadding)) {
        if (getPlatform() == Platform.Android) {
            // On a short window (#13) Queue keeps only the dock: with the transport as well its list
            // had no room at all. The cover's page, and its transport, is one tap away on the dock.
            if (!(state.isShort && viewState == ImmersiveView.QUEUE)) {
                ImmersivePlaybackControls(state = state, actions = actions, typography = typography)
                Spacer(modifier = Modifier.height(if (state.isShort) 12.dp else 18.dp))
            }
            // A short window (#13) leaves the volume to the phone's buttons, and the output's name
            // under the dock to the output sheet.
            deviceVolumeController?.takeUnless { state.isShort }?.let { controller ->
                ImmersiveVolumeRow(controller = controller)
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
        ImmersiveActionRow(
            viewState = viewState,
            onSelectView = onSelectView,
            lyricsAvailable = state.screenData.lyricsData != null,
            activeColor = activePillContainer,
            activeContentColor = activePillContent,
            onOpenOutput = onOpenOutput,
            onOpenListenTogether = actions.onOpenListenTogether,
        )
        if (!state.isShort) {
            Spacer(modifier = Modifier.height(14.dp))
            // Crossfaded, since it changes on its own — a headset connecting renames the line under the
            // user's thumb. Tappable: it names the output, so it opens the output sheet.
            Crossfade(
                targetState = outputName.orEmpty(),
                animationSpec = tween(300),
                label = "immersiveOutputCaption",
                modifier = Modifier.fillMaxWidth(),
            ) { name ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = name,
                        style = typography.footer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth(0.65f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onOpenOutput,
                                ),
                    )
                }
            }
        }
        Spacer(
            modifier =
                Modifier.height(
                    // Room under the caption, above the gesture bar — the owner asked for the whole
                    // block to sit higher than the bare inset put it.
                    with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() } + if (state.isShort) 8.dp else 36.dp,
                ),
        )
    }
}
