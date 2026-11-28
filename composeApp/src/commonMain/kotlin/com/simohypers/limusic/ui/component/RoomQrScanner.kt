package com.simohypers.limusic.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.simohypers.limusic.expect.ui.CameraDialogWindow
import com.simohypers.limusic.expect.ui.CameraTrouble
import com.simohypers.limusic.expect.ui.QrCameraPreview
import com.simohypers.limusic.expect.ui.QrSighting
import com.simohypers.limusic.expect.ui.encodeQr
import com.simohypers.limusic.expect.ui.rememberCameraPermission
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.QrCodeScanner
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.close
import limusic.composeapp.generated.resources.ltd_camera_allow
import limusic.composeapp.generated.resources.ltd_camera_blocked
import limusic.composeapp.generated.resources.ltd_camera_failed
import limusic.composeapp.generated.resources.ltd_camera_needed
import limusic.composeapp.generated.resources.ltd_camera_none
import limusic.composeapp.generated.resources.ltd_camera_settings
import limusic.composeapp.generated.resources.ltd_scan_as
import limusic.composeapp.generated.resources.ltd_scan_found
import limusic.composeapp.generated.resources.ltd_scan_hint
import limusic.composeapp.generated.resources.ltd_scan_not_room
import limusic.composeapp.generated.resources.ltd_scan_title
import limusic.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/**
 * The full-screen camera that joins a Listen Together room from its QR code. It opens as a circle
 * growing out of [origin] (the camera button's centre on screen) and closes back into it. The
 * first room link it reads wins: the frame flies onto the code, the phone ticks, and after a beat
 * the scanner fades and hands the invite to [onRoom]. Any other QR code gets a short "not a room"
 * line and scanning goes on.
 */
@Composable
fun RoomQrScanner(
    name: String,
    origin: Offset?,
    onRoom: (invite: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val open = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var closing by remember { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                open.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                onDismiss()
            }
        }
    }
    LaunchedEffect(Unit) { open.animateTo(1f, tween(460, easing = FastOutSlowInEasing)) }

    var camera by remember { mutableStateOf(CameraStep.Asking) }
    var retries by remember { mutableIntStateOf(0) }
    val permission =
        rememberCameraPermission { granted, blocked ->
            camera =
                when {
                    granted -> CameraStep.Ready
                    blocked -> CameraStep.Blocked
                    else -> CameraStep.Denied
                }
        }
    LaunchedEffect(Unit) {
        camera =
            when {
                !permission.hasCamera -> CameraStep.None
                permission.granted -> CameraStep.Ready
                else -> CameraStep.Asking.also { permission.request() }
            }
    }
    // Back from the system settings with the camera allowed.
    LifecycleResumeEffect(camera) {
        if ((camera == CameraStep.Denied || camera == CameraStep.Blocked) && permission.granted) camera = CameraStep.Ready
        onPauseOrDispose { }
    }

    var found by remember { mutableStateOf<Found?>(null) }
    val lock = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    var notRoomSeen by remember { mutableIntStateOf(0) }
    var notRoom by remember { mutableStateOf(false) }
    LaunchedEffect(notRoomSeen) {
        if (notRoomSeen == 0) return@LaunchedEffect
        notRoom = true
        delay(2200)
        notRoom = false
    }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        CameraDialogWindow()
        val density = LocalDensity.current
        var rootOnScreen by remember { mutableStateOf(Offset.Zero) }
        val accent = MaterialTheme.colorScheme.run { if (primary.luminance() < 0.3f) inversePrimary else primary }
        val breath by rememberInfiniteTransition().animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { rootOnScreen = it.positionOnScreen() }
                .graphicsLayer {
                    val t = open.value
                    alpha = fade.value
                    if (t < 1f) {
                        val c = origin?.minus(rootOnScreen) ?: Offset(size.width / 2f, size.height)
                        val far = max(max(hypot(c.x, c.y), hypot(size.width - c.x, c.y)), max(hypot(c.x, size.height - c.y), hypot(size.width - c.x, size.height - c.y)))
                        shape = CircleAt(c, far * t)
                        clip = true
                    } else {
                        shape = RectangleShape
                        clip = false
                    }
                }.background(Color.Black),
        ) {
            val idleSide = min(maxWidth * 0.7f, 300.dp)
            val idleCenterY = maxHeight * 0.42f
            val idle =
                with(density) {
                    Frame(Offset(constraints.maxWidth / 2f, idleCenterY.toPx()), idleSide.toPx(), 0f)
                }

            if (camera == CameraStep.Ready) {
                key(retries) {
                    QrCameraPreview(
                        onSighting = { sighting ->
                            if (found != null || closing) return@QrCameraPreview
                            val invite = ListenTogetherViewModel.inviteOfLink(sighting.text)
                            if (invite == null) {
                                notRoomSeen++
                                return@QrCameraPreview
                            }
                            val frame = sighting.frame(density) ?: idle.copy(side = idle.side * 0.9f)
                            found = Found(invite, frame)
                            notRoom = false
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            scope.launch {
                                launch { lock.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 320f)) }
                                delay(180)
                                launch { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                                delay(620)
                                fade.animateTo(0f, tween(220, easing = LinearOutSlowInEasing))
                                onRoom(invite)
                            }
                        },
                        onTrouble = { camera = if (it == CameraTrouble.None) CameraStep.None else CameraStep.Failed },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // The dimmed surround with the code's window cut out, and the frame's corners.
            val lockColor = accent
            Spacer(
                Modifier.fillMaxSize().drawBehind {
                    val l = lock.value
                    val target = found?.frame ?: idle
                    val c = lerp(idle.center, target.center, l)
                    val side = lerp(idle.side * breath, target.side, l.coerceIn(0f, 1.2f))
                    val angle = lerp(0f, target.angle, l)
                    val radius = side * 0.11f
                    val hole = Path().apply { addRoundRect(RoundRect(Rect(c, side / 2f), CornerRadius(radius))) }
                    val reach = hypot(size.width, size.height)
                    rotate(angle, c) {
                        clipPath(hole, ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.58f), topLeft = Offset(-reach, -reach), size = Size(size.width + 2 * reach, size.height + 2 * reach))
                        }
                        drawCorners(c, side, radius, lerp(Color.White, lockColor, l.coerceIn(0f, 1f)), 4.dp.toPx())
                    }
                },
            )

            // The tick that lands on the code.
            found?.let { f ->
                val badge = 56.dp
                Box(
                    Modifier
                        .offset { (f.frame.center - Offset(badge.toPx() / 2f, badge.toPx() / 2f)).round() }
                        .size(badge)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            alpha = pop.value.coerceIn(0f, 1f)
                        }.clip(CircleShape)
                        .background(lockColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LimusicIcons.Check, contentDescription = null, tint = Color.Black.copy(alpha = 0.85f), modifier = Modifier.size(30.dp))
                }
            }

            // What the camera can't do, inside the frame.
            if (camera != CameraStep.Ready && camera != CameraStep.Asking) {
                Problem(
                    camera = camera,
                    onAllow = permission::request,
                    onSettings = permission::openSettings,
                    onRetry = {
                        retries++
                        camera = CameraStep.Ready
                    },
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = idleCenterY - idleSide / 2f)
                            .size(idleSide),
                )
            }

            // Title and hint under the frame.
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = idleCenterY + idleSide / 2f + 28.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = found?.invite,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { it / 3 }) togetherWith (fadeOut(tween(120)) + slideOutVertically { -it / 3 })
                    },
                ) { invite ->
                    Text(
                        if (invite == null) {
                            stringResource(Res.string.ltd_scan_title)
                        } else {
                            stringResource(Res.string.ltd_scan_found, invite.substringBefore('@').chunked(4).joinToString(" "))
                        },
                        style = typo().titleMedium,
                        color = if (invite == null) Color.White else lockColor,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(Res.string.ltd_scan_hint),
                    style = typo().bodySmall,
                    color = Color.White.copy(alpha = if (found == null) 0.7f else 0f),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedVisibility(
                    visible = notRoom && found == null,
                    enter = fadeIn(tween(160)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(200)),
                ) {
                    Pill(stringResource(Res.string.ltd_scan_not_room), Color(0xFFFFB4AB), Color(0xFF93000A).copy(alpha = 0.55f))
                }
                Pill(stringResource(Res.string.ltd_scan_as, name), Color.White, Color.White.copy(alpha = 0.14f))
            }

            IconButton(
                onClick = close,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
            ) {
                Icon(LimusicIcons.Close, contentDescription = stringResource(Res.string.close), tint = Color.White)
            }
        }
    }
}

private enum class CameraStep { Asking, Ready, Denied, Blocked, None, Failed }

/** Where the frame sits: centre and side in the preview's pixels, turned by [angle] degrees. */
private data class Frame(
    val center: Offset,
    val side: Float,
    val angle: Float,
)

private class Found(
    val invite: String,
    val frame: Frame,
)

/**
 * The frame around the code from its three eyes, whose centres sit 3.5 modules in from the edges.
 * The module count is the room link's own (this app drew the code), with a little margin around.
 */
private fun QrSighting.frame(density: Density): Frame? {
    val (bottomLeft, topLeft, topRight) = eyes ?: return null
    val span = ((topRight - topLeft).getDistance() + (bottomLeft - topLeft).getDistance()) / 2f
    if (span <= 0f) return null
    val modules = (encodeQr(text)?.size ?: 33).toFloat()
    val margin = with(density) { 14.dp.toPx() }
    val side = span * modules / (modules - 7f) + 2 * margin
    val turn = (atan2(topRight.y - topLeft.y, topRight.x - topLeft.x) * 180.0 / PI).toFloat()
    // A square looks the same every quarter turn: take the smallest turn that lands on it.
    val angle = ((turn % 90f) + 135f) % 90f - 45f
    return Frame((bottomLeft + topRight) / 2f, side, angle)
}

/** The frame's four corners: a rounded bend with a short arm each way. */
private fun DrawScope.drawCorners(
    center: Offset,
    side: Float,
    radius: Float,
    color: Color,
    width: Float,
) {
    val half = side / 2f
    val arm = side * 0.14f
    val stroke = Stroke(width = width, cap = StrokeCap.Round)
    for ((sx, sy) in listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)) {
        val corner = Offset(center.x + sx * half, center.y + sy * half)
        val path =
            Path().apply {
                moveTo(corner.x, corner.y - sy * (radius + arm))
                lineTo(corner.x, corner.y - sy * radius)
                quadraticTo(corner.x, corner.y, corner.x - sx * radius, corner.y)
                lineTo(corner.x - sx * (radius + arm), corner.y)
            }
        drawPath(path, color, style = stroke)
    }
}

/** A circle of [radius] around [center], as a rounded square so the GPU can clip to it. */
private class CircleAt(
    private val center: Offset,
    private val radius: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = Outline.Rounded(RoundRect(Rect(center, radius), CornerRadius(radius)))
}

@Composable
private fun Pill(
    text: String,
    color: Color,
    background: Color,
) {
    Text(
        text,
        style = typo().labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(background)
                .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun Problem(
    camera: CameraStep,
    onAllow: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(LimusicIcons.QrCodeScanner, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(
                when (camera) {
                    CameraStep.Blocked -> Res.string.ltd_camera_blocked
                    CameraStep.None -> Res.string.ltd_camera_none
                    CameraStep.Failed -> Res.string.ltd_camera_failed
                    else -> Res.string.ltd_camera_needed
                },
            ),
            style = typo().bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        val action =
            when (camera) {
                CameraStep.Denied -> Res.string.ltd_camera_allow to onAllow
                CameraStep.Blocked -> Res.string.ltd_camera_settings to onSettings
                CameraStep.Failed -> Res.string.retry to onRetry
                else -> null
            }
        action?.let { (label, onClick) ->
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) { Text(stringResource(label), color = LocalContentColor.current) }
        }
    }
}

private fun Offset.round() = IntOffset(x.toInt(), y.toInt())
