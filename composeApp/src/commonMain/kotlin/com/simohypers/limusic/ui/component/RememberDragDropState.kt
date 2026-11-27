package com.simohypers.limusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.ui.icon.DragHandle
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.rounded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.drag_to_reorder
import limusic.composeapp.generated.resources.move_down
import limusic.composeapp.generated.resources.move_up
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.min

/**
 * [onSwap] gets the dragged row's lazy index before the drag and where it was dropped; [canMoveTo]
 * says which lazy indexes a row may be dragged over (headers and the playing row are not). Both are
 * the latest the caller passed, not the first: they read the caller's current list.
 *
 * A row may not be drawn above the first row it can move to, when a row it can't pass shows above
 * that one or it starts the list. [onBlocked] runs once per drag, the first time the row is pulled
 * well past that point, so the caller can say why it stops.
 *
 * [coveredTop] and [coveredBottom] are how much of the list's top and bottom something else draws
 * over (a bar, the mini player), beyond its own content padding: the edge scroll starts where the
 * list can still be seen, not under them.
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    coveredTop: Dp = 0.dp,
    coveredBottom: Dp = 0.dp,
    canMoveTo: (Int) -> Boolean = { true },
    onBlocked: () -> Unit = {},
    onSwap: (Int, Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val state =
        remember(lazyListState) {
            DragDropState(
                state = lazyListState,
                scope = scope,
                maxScrollPerFramePx = with(density) { AUTO_SCROLL_MAX_PER_FRAME.toPx() },
            )
        }
    state.onSwap = onSwap
    state.canMoveTo = canMoveTo
    state.onBlocked = onBlocked
    state.coveredTopPx = with(density) { coveredTop.toPx() }
    state.coveredBottomPx = with(density) { coveredBottom.toPx() }
    state.topMarginPx = with(density) { AUTO_SCROLL_TOP_MARGIN.toPx() }
    return state
}

fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? =
    this
        .layoutInfo
        .visibleItemsInfo
        .getOrNull(
            absoluteIndex -
                this.layoutInfo.visibleItemsInfo
                    .first()
                    .index,
        )

val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

// The edge scroll is RecyclerView's ItemTouchHelper's (androidx.recyclerview, Apache-2.0): the most
// the list scrolls in one 60 Hz frame (item_touch_helper_max_drag_scroll_per_frame, 20dp), reached
// once the row is a whole row height past the edge and has been held there for
// AUTO_SCROLL_ACCELERATION_MS.
private val AUTO_SCROLL_MAX_PER_FRAME = 20.dp
private const val AUTO_SCROLL_ACCELERATION_MS = 2000L
private const val AUTO_SCROLL_FRAME_MS = 1000f / 60f

// Our deviation from AndroidX: ItemTouchHelper scales by t^5 of the held time alone, which is near
// zero for the first ~0.7 s, so the list barely moves at first. The time factor starts at this
// fraction of the full speed instead and still eases in to all of it: FLOOR + (1 - FLOOR) * t^5.
private const val AUTO_SCROLL_MIN_SPEED_FRACTION = 0.15f

// The edge scroll upwards starts this far below the list's visible top, a little before the row
// reaches it. The bottom edge has none.
private val AUTO_SCROLL_TOP_MARGIN = 32.dp

// How far, as a fraction of its height, the row in hand is pulled past where it has to stop before
// onBlocked runs: a row resting there that moves a few px doesn't count.
private const val BLOCKED_PULL_FRACTION = 0.5f

/**
 * A reorderable row. The one picked up lifts (a tonal card, a shadow, a little larger, above the
 * others) and follows the finger exactly; the others slide out of its way. Dropped, it springs into
 * its slot and settles back down.
 *
 * [key] is the row's lazy key: the row is recognised by it, so it stays the dragged one while its
 * index changes under it.
 */
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    key: Any,
    modifier: Modifier,
    /** What the lifted row is drawn on: [tonalLift] on a solid background, [translucentLift] over the cover. */
    liftColor: Color = tonalLift(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface),
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit,
) {
    val dragging = key == dragDropState.draggingKey
    val settling = key == dragDropState.settlingKey
    val liftState = animateFloatAsState(if (dragging) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "rowLift")
    val raised by remember { derivedStateOf { liftState.value > 0f } }
    val shape = rounded(8.dp)
    // A see-through lift shows the shadow under it too: a fainter one keeps the cover's colours.
    val shadowColor = if (liftColor.alpha < 1f) SOFT_SHADOW else DefaultShadowColor
    // The row in hand stays composed while the list scrolls its slot out of view, so the drag its
    // handle runs goes on.
    val pinnable = LocalPinnableContainer.current
    DisposableEffect(dragging, pinnable) {
        val handle = if (dragging) pinnable?.pin() else null
        onDispose { handle?.release() }
    }
    Column(
        modifier =
            Modifier
                // The row being moved places itself (through the translation below): a placement
                // animation on it as well would pull it away from the finger on every swap.
                .animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec =
                        if (dragging || settling) {
                            null
                        } else {
                            spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
                        },
                ).zIndex(if (dragging || settling || raised) 1f else 0f)
                .then(modifier)
                // Read at draw time, so following the finger doesn't recompose the row.
                .graphicsLayer {
                    val lift = liftState.value
                    translationY = dragDropState.translationOf(key)
                    scaleX = 1f + LIFT_SCALE * lift
                    scaleY = 1f + LIFT_SCALE * lift
                    shadowElevation = LIFT_ELEVATION.toPx() * lift
                    ambientShadowColor = shadowColor
                    spotShadowColor = shadowColor
                    this.shape = shape
                    clip = false
                }.drawBehind {
                    val lift = liftState.value
                    if (lift > 0f) {
                        val radius = shape.topStart.toPx(size, this)
                        drawRoundRect(liftColor, cornerRadius = CornerRadius(radius), alpha = lift)
                    }
                },
    ) {
        content(dragging)
    }
}

// A row's drag handle: full height to grab, narrower than 48dp so it sits closer to the row's ⋯.
internal val DragHandleWidth = 36.dp

private const val LIFT_SCALE = 0.03f
private val LIFT_ELEVATION = 8.dp
private val SOFT_SHADOW = Color.Black.copy(alpha = 0.35f)

/**
 * The lift for rows drawn over the cover or a blur: a veil in the rows' own [content] colour, so the
 * cover still shows through and the row only reads as picked up.
 */
fun translucentLift(content: Color): Color = content.copy(alpha = 0.12f)

/**
 * The lift for rows on a solid [background]: that background a step towards the rows' [content]
 * colour, as Material's tonal elevation does, so the hue stays the list's own.
 */
fun tonalLift(
    background: Color,
    content: Color,
): Color = content.copy(alpha = 0.08f).compositeOver(background)

/**
 * The handle at the end of a reorderable row. It starts the move as soon as it is touched, no long
 * press, and keeps the pointer to itself until it lifts, so the row's swipe, the list's scroll and
 * the sheet underneath never see the drag. [onMoveUp] and [onMoveDown] are the same move offered to
 * accessibility services, so reordering never depends on dragging; null where there is nowhere to go.
 */
@Composable
fun ReorderHandle(
    dragDropState: DragDropState,
    /** The row's current lazy index. */
    index: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Shown: it comes out from under the row's ⋯ and goes back into it ([handleEnter], [handleExit]). */
    visible: Boolean = true,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(visible, enter = handleEnter(rtl), exit = handleExit(rtl)) {
        ReorderHandleBox(dragDropState, index, onMoveUp, onMoveDown, modifier)
    }
}

// A row's drag handle sits just before its ⋯, at the row's end: it fades in sliding out from under
// the ⋯ while its room opens, and folds back the same way.
internal fun handleEnter(rtl: Boolean): EnterTransition =
    fadeIn() + expandHorizontally(expandFrom = Alignment.End) + slideInHorizontally { if (rtl) -it else it }

internal fun handleExit(rtl: Boolean): ExitTransition =
    fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End) + slideOutHorizontally { if (rtl) -it else it }

@Composable
private fun ReorderHandleBox(
    dragDropState: DragDropState,
    index: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier,
) {
    val currentIndex by rememberUpdatedState(index)
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val label = stringResource(Res.string.drag_to_reorder)
    val upLabel = stringResource(Res.string.move_up)
    val downLabel = stringResource(Res.string.move_down)
    Box(
        modifier
            .size(width = DragHandleWidth, height = 48.dp)
            .onGloballyPositioned { coordinates[0] = it }
            .pointerInput(dragDropState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val owner = Any()
                    if (!dragDropState.onDragStartAt(currentIndex, owner)) return@awaitEachGesture
                    // Taken here so the row's tap doesn't play the song when the handle is let go.
                    down.consume()
                    // The handle rides on the row it moves: measured in its own coordinates the
                    // finger would barely move, so distances are taken in the root's.
                    var lastY = coordinates[0]?.takeIf { it.isAttached }?.localToRoot(down.position)?.y ?: down.position.y
                    var dropped = false
                    try {
                        dropped =
                            drag(down.id) { change ->
                                val y = coordinates[0]?.takeIf { it.isAttached }?.localToRoot(change.position)?.y ?: lastY
                                dragDropState.onDrag(Offset(0f, y - lastY), owner)
                                lastY = y
                                change.consume()
                            }
                        currentEvent.changes.forEach { it.consume() }
                    } finally {
                        dragDropState.onDragInterrupted(end = dropped, owner = owner)
                    }
                }
            }.semantics {
                contentDescription = label
                customActions =
                    listOfNotNull(
                        onMoveUp?.let { up ->
                            CustomAccessibilityAction(upLabel) {
                                up()
                                true
                            }
                        },
                        onMoveDown?.let { down ->
                            CustomAccessibilityAction(downLabel) {
                                down()
                                true
                            }
                        },
                    )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(LimusicIcons.DragHandle, contentDescription = null)
    }
}

/**
 * Hosts a list whose long-press reorder sits in [gesture], with a quicker long press than the
 * system's: the reorder is the only long press there, and half a second felt like nothing happens.
 * A delay the user lengthened in the accessibility settings is kept. Only [gesture] sees the change:
 * [content] gets the system's back, so nothing in the rows presses for less.
 */
@Composable
fun QuickLongPressBox(
    gesture: Modifier,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val system = LocalViewConfiguration.current
    val quick =
        remember(system) {
            if (system.longPressTimeoutMillis > DEFAULT_LONG_PRESS_MS) system else QuickLongPressConfiguration(system)
        }
    CompositionLocalProvider(LocalViewConfiguration provides quick) {
        Box(modifier.then(gesture)) {
            CompositionLocalProvider(LocalViewConfiguration provides system) { content() }
        }
    }
}

/**
 * A locked list's long press: [onLongPress] gets where it landed and says whether it took it. A
 * taken press is eaten from there on, before the rows see it, so letting go doesn't also click the
 * row under the finger and dragging on doesn't scroll the list or swipe the row away.
 */
suspend fun PointerInputScope.detectLockedLongPress(onLongPress: (Offset) -> Boolean) =
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        if (!onLongPress(press.position)) return@awaitEachGesture
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }

private const val QUICK_LONG_PRESS_MS = 300L
private const val DEFAULT_LONG_PRESS_MS = 500L

@OptIn(ExperimentalComposeUiApi::class)
private class QuickLongPressConfiguration(
    system: ViewConfiguration,
) : ViewConfiguration by system {
    override val longPressTimeoutMillis: Long = minOf(QUICK_LONG_PRESS_MS, system.longPressTimeoutMillis)
}

/**
 * A drag reorders the rows on screen as it goes ([arrange]) and tells the caller only once, on the
 * drop ([onSwap]): one move for the player, not one per row passed. The new order stays on screen
 * until the caller's list comes back with it, so the rows don't jump back and forth meanwhile.
 *
 * Every drag has an owner (the list's long press, or a row's handle): only the owner moves or ends
 * it, so the list's long press firing under a held handle can't take the drag over or cancel it.
 */
class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val maxScrollPerFramePx: Float,
) {
    internal var onSwap: (Int, Int) -> Unit = { _, _ -> }
    internal var canMoveTo: (Int) -> Boolean = { true }
    internal var onBlocked: () -> Unit = {}
    internal var coveredTopPx = 0f
    internal var coveredBottomPx = 0f
    internal var topMarginPx = 0f

    // Whether onBlocked has run in this drag.
    private var blockedShown = false

    private var owner: Any? = null
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    // used to obtain initial offsets on drag start
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var draggingKey by mutableStateOf<Any?>(null)
        private set

    // The move on screen: the row's index before the drag, and where it is now.
    private var move by mutableStateOf<Pair<Int, Int>?>(null)

    // The caller's list as [arrange] last saw it, and as it was at the drop.
    private var lastSource: List<Any?>? = null
    private var movedSource: List<Any?>? = null

    // The dropped row on its way into its slot: where it was let go, and how much of that is left.
    internal var settlingKey by mutableStateOf<Any?>(null)
        private set
    private var settleFrom = 0f
    private val settleProgress = Animatable(0f)
    private var generation = 0
    private var autoScrollJob: Job? = null

    // When the current run of edge-scrolling frames began (frame time, ns); null while not scrolling.
    private var autoScrollStartNanos: Long? = null

    /** The caller's list in the order on screen: [source] with the row being dragged moved. [base] is the first row's lazy index. */
    fun <T> arrange(
        source: List<T>,
        base: Int = 0,
    ): List<T> {
        val (from, to) = move ?: return source
        val dragging = currentIndexOfDraggedItem != null
        if (dragging && lastSource != null && lastSource != source) {
            // The queue changed under the drag (a song ended, the host moved it): the indexes no
            // longer name the rows they did, so drop it rather than move the wrong song.
            scope.launch { cancel() }
            return source
        }
        lastSource = source
        // Dropped, and the caller's list has come back changed: that is the new order.
        if (!dragging && source != movedSource) return source
        val f = from - base
        val t = to - base
        if (f == t || f !in source.indices || t !in source.indices) return source
        return source.toMutableList().apply { add(t, removeAt(f)) }
    }

    /** The lazy item under [offset], a position in the list's own coordinates. */
    fun itemAt(offset: Offset): LazyListItemInfo? {
        // Item offsets start after the top content padding; the pointer's start at the list's edge.
        val y = offset.y.toInt() - state.layoutInfo.beforeContentPadding
        return state.layoutInfo.visibleItemsInfo.firstOrNull { item -> y in item.offset..item.offsetEnd }
    }

    fun onDragStart(
        offset: Offset,
        owner: Any = LIST,
    ): Boolean = itemAt(offset)?.let { start(it, owner) } ?: false

    fun onDragStartAt(
        index: Int,
        owner: Any,
    ): Boolean = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.let { start(it, owner) } ?: false

    private fun start(
        item: LazyListItemInfo,
        owner: Any,
    ): Boolean {
        if (this.owner != null || !canMoveTo(item.index)) return false
        // The last drop not yet back from the caller: these indexes are still the old list's.
        if (move != null && lastSource == movedSource) return false
        this.owner = owner
        generation++
        settlingKey = null
        currentIndexOfDraggedItem = item.index
        draggingKey = item.key
        initiallyDraggedElement = item
        draggingItemInitialOffset = item.offset
        draggedDistance = 0f
        blockedShown = false
        lastSource = null
        move = item.index to item.index
        return true
    }

    fun onDragInterrupted(
        end: Boolean = false,
        owner: Any = LIST,
    ) {
        if (owner !== this.owner) return
        this.owner = null
        autoScrollJob?.cancel()
        autoScrollJob = null
        autoScrollStartNanos = null
        val (from, to) = move ?: (0 to 0)
        val committed = end && from != to && from >= 0 && to >= 0
        settleFrom = draggedTop()
        settlingKey = draggingKey
        if (committed) {
            Logger.d("QueueBottomSheet", "onDragInterrupted: $from, $to")
            movedSource = lastSource
            onSwap(from, to)
        } else {
            move = null
        }
        draggingItemInitialOffset = 0
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        draggingKey = null
        initiallyDraggedElement = null
        val drop = ++generation
        scope.launch {
            settleProgress.snapTo(1f)
            settleProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            if (generation == drop) settlingKey = null
        }
        if (committed) {
            scope.launch {
                // Should the caller's list never come back changed (a player that ignored the
                // move), show its order again rather than one that isn't real.
                delay(PENDING_TIMEOUT_MS)
                if (generation == drop) move = null
            }
        }
    }

    private fun cancel() {
        owner?.let { onDragInterrupted(end = false, owner = it) }
    }

    /** Moves the row in hand, and scrolls the list on while it is held against its top or bottom edge. */
    fun onDrag(
        offset: Offset,
        owner: Any = LIST,
    ) {
        if (owner !== this.owner) return
        draggedDistance += offset.y
        moveIfOverAnother()
        checkBlocked()
        if (autoScrollJob?.isActive != true && edgeOvershoot() != 0f) {
            autoScrollJob = scope.launch { autoScroll() }
        }
    }

    // The top of the first row on screen a row may move to, when the rows above it can't be passed
    // or it starts the list. Null when that edge is scrolled out of view: the row is free until the
    // edge scroll brings it back.
    private fun boundaryTop(): Float? {
        val visible = state.layoutInfo.visibleItemsInfo
        val i = visible.indexOfFirst { canMoveTo(it.index) }
        if (i < 0 || (i == 0 && visible[0].index != 0)) return null
        return visible[i].offset.toFloat()
    }

    // Where the row in hand is drawn, as a row offset: under the finger, but no higher than boundaryTop.
    private fun draggedTop(): Float {
        val top = draggingItemInitialOffset + draggedDistance
        val boundary = boundaryTop() ?: return top
        return maxOf(top, boundary)
    }

    // Runs onBlocked, once per drag, when the finger has pulled the row well above where it stops.
    // Also run after every edge scroll step: the boundary may scroll into view under a still finger.
    private fun checkBlocked() {
        if (blockedShown || currentIndexOfDraggedItem == null) return
        val size = initiallyDraggedElement?.size ?: return
        val boundary = boundaryTop() ?: return
        if (draggingItemInitialOffset + draggedDistance < boundary - size * BLOCKED_PULL_FRACTION) {
            blockedShown = true
            onBlocked()
        }
    }

    // Once the middle of the row in hand is over another row, it takes that row's place.
    private fun moveIfOverAnother() {
        val current = currentIndexOfDraggedItem ?: return
        val visible = state.layoutInfo.visibleItemsInfo
        val slot = visible.firstOrNull { it.index == current } ?: return
        val middle = (draggedTop() + slot.size / 2f).toInt()
        val target =
            visible.firstOrNull { it.index != current && middle in it.offset..it.offsetEnd && canMoveTo(it.index) }
                ?: return
        // The list keeps its first row's key in place: moving that row would scroll the list with it.
        if (current == state.firstVisibleItemIndex || target.index == state.firstVisibleItemIndex) {
            state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }
        move = move?.let { it.first to target.index }
        currentIndexOfDraggedItem = target.index
    }

    /** How far the row with [key] is drawn from its slot: under the finger while dragged, on its way home once dropped. */
    internal fun translationOf(key: Any): Float {
        val dragging = key == draggingKey
        if (!dragging && key != settlingKey) return 0f
        val slot = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
        return if (dragging) {
            draggedTop() - slot.offset
        } else {
            (settleFrom - slot.offset) * settleProgress.value
        }
    }

    /**
     * How far the row in hand, where it is drawn now, reaches past the list's visible top (negative)
     * or bottom (positive), in px; 0 when it doesn't. The visible top and bottom are inside its
     * content padding or what covers it, whichever reaches further. As in ItemTouchHelper's
     * scrollIfNecessary: the dragged row's own edges, not the finger, and the top only once the drag
     * has gone up overall, the bottom only once it has gone down.
     */
    private fun edgeOvershoot(): Float {
        if (currentIndexOfDraggedItem == null) return 0f
        val size = initiallyDraggedElement?.size ?: return 0f
        val info = state.layoutInfo
        // Its drawn top measured from the list's top edge: item offsets start after the top padding.
        val rowTop = draggedTop() - info.viewportStartOffset
        val rowBottom = rowTop + size
        val visibleTop = maxOf(info.beforeContentPadding.toFloat(), coveredTopPx) + topMarginPx
        val visibleBottom = info.viewportSize.height - maxOf(info.afterContentPadding.toFloat(), coveredBottomPx)
        val topDiff = rowTop - visibleTop
        val bottomDiff = rowBottom - visibleBottom
        return when {
            draggedDistance < 0f && topDiff < 0f -> topDiff
            draggedDistance > 0f && bottomDiff > 0f -> bottomDiff
            else -> 0f
        }
    }

    // ItemTouchHelper.interpolateOutOfBoundsScroll's curves: px to scroll in one 60 Hz frame, eased
    // out by how far past the edge the row is (a whole row height is the most) and eased in, steeply,
    // by how long the edge scroll has been going, from AUTO_SCROLL_MIN_SPEED_FRACTION rather than zero.
    private fun interpolateOutOfBoundsScroll(
        viewSize: Int,
        viewSizeOutOfBounds: Float,
        msSinceStartScroll: Long,
    ): Float {
        val direction = if (viewSizeOutOfBounds > 0f) 1 else -1
        val outOfBoundsRatio = if (viewSize > 0) min(1f, abs(viewSizeOutOfBounds) / viewSize) else 1f
        val u = outOfBoundsRatio - 1f
        val cappedScroll = (direction * maxScrollPerFramePx * (u * u * u * u * u + 1f)).toInt()
        val t = min(1f, msSinceStartScroll.toFloat() / AUTO_SCROLL_ACCELERATION_MS)
        val timeFactor = AUTO_SCROLL_MIN_SPEED_FRACTION + (1f - AUTO_SCROLL_MIN_SPEED_FRACTION) * (t * t * t * t * t)
        val value = (cappedScroll * timeFactor).toInt()
        return if (value == 0) direction.toFloat() else value.toFloat()
    }

    // Scrolls a little every frame while the row in hand reaches past an edge it is dragged towards.
    // The timer starts on the first frame that scrolls, so every edge scroll starts slow. The amount
    // is per 16.67 ms, scaled by the real frame time. Stops when the row is back inside, the list
    // can't go further, or the drag ends (onDragInterrupted cancels it).
    private suspend fun autoScroll() {
        var lastFrame = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val frameMs = ((now - lastFrame) / 1_000_000f).coerceAtMost(50f)
            lastFrame = now
            val overshoot = edgeOvershoot()
            val size = initiallyDraggedElement?.size
            if (overshoot == 0f || size == null) break
            val start = autoScrollStartNanos ?: now.also { autoScrollStartNanos = it }
            val perFrame = interpolateOutOfBoundsScroll(size, overshoot, (now - start) / 1_000_000L)
            val consumed = state.scrollBy(perFrame * frameMs / AUTO_SCROLL_FRAME_MS)
            if (consumed == 0f && frameMs > 0f) break
            // The rows scrolled under a finger that may be holding still.
            moveIfOverAnother()
            checkBlocked()
        }
        autoScrollStartNanos = null
    }

    private companion object {
        // The owner of a drag started by the list's own long press.
        val LIST = Any()
        const val PENDING_TIMEOUT_MS = 2_000L
    }
}
