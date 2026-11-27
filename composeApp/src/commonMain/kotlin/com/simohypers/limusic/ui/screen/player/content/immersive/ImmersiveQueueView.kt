@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.simohypers.limusic.ui.screen.player.content.immersive

import com.simohypers.limusic.ui.component.QueueLockIcon
import limusic.composeapp.generated.resources.drag_to_reorder
import com.simohypers.limusic.ui.icon.Lock
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.mediaservice.handler.RepeatState
import com.simohypers.limusic.expect.ui.DeviceVolumeController
import com.simohypers.limusic.ui.component.DraggableItem
import com.simohypers.limusic.ui.component.QuickLongPressBox
import com.simohypers.limusic.ui.component.ReorderHandle
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.SwipeToRemoveQueueRow
import com.simohypers.limusic.ui.component.rememberDragDropState
import com.simohypers.limusic.ui.component.translucentLift
import com.simohypers.limusic.ui.icon.Info
import com.simohypers.limusic.ui.icon.PlaylistAdd
import com.simohypers.limusic.ui.icon.Repeat
import com.simohypers.limusic.ui.icon.RepeatOne
import com.simohypers.limusic.ui.icon.Shuffle
import com.simohypers.limusic.ui.icon.shuffleIcon
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentActions
import com.simohypers.limusic.ui.screen.player.content.NowPlayingContentState
import com.simohypers.limusic.viewModel.UIEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.continue_playing
import limusic.composeapp.generated.resources.endless_queue
import limusic.composeapp.generated.resources.now_playing
import limusic.composeapp.generated.resources.queue_move_below_playing

/**
 * The QUEUE body: compact header, [Info][PlaylistAdd][Shuffle][Repeat] pills, a "Continue
 * Playing" section (with the endless-queue switch) and the upcoming tracks — the OLD queue
 * sheet's own [SongFullWidthItems] rows with its long-press-drag reorder and its per-item ⋯
 * sheet (the full song sheet plus move up/down/to play next and delete), matching
 * [com.simohypers.limusic.ui.component.QueueBottomSheet] exactly.
 * Ends in the same fixed bottom cluster as MAIN/LYRICS.
 */
@Composable
internal fun ImmersiveQueueView(
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
    dataStoreManager: DataStoreManager = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject(),
) {
    val localDensity = LocalDensity.current

    // Upcoming tracks live in the SAME index space as musicServiceHandler.moveInQueue(from, to) and
    // removeMediaItem(index): artworkQueue == queueData.data.listTracks, and both operations
    // read/write that list (and the player timeline) at the SAME position — confirmed by reading
    // MediaServiceHandlerImpl.removeMediaItem/swap and ExoPlayerAdapter.moveMediaItem/
    // removeMediaItem/getUnshuffledIndex, which all treat their index argument as "current
    // shuffle/display order", i.e. exactly artworkQueue's own order. `offset` converts a position
    // within this UPCOMING-ONLY sublist back to that absolute space.
    val offset = state.currentOrderIndex + 1
    // withIndex() BEFORE drop(), so every row carries the index it had in the full queue.
    //
    // This used to be a plain drop() with `offset + localIndex` added back at click time, and that
    // is exactly where the Immersive queue diverged from QueueBottomSheet — which never computes
    // an index at all, it reads the one itemsIndexed hands it over the whole list. Adding the
    // offset back makes every row's identity depend on state.currentOrderIndex, a value DERIVED in
    // the shell; the moment that derivation is off by anything, the row the user tapped and the
    // index sent to the player are two different songs. Carrying the original index removes the
    // arithmetic, so a wrong currentOrderIndex can now only cut the list in the wrong PLACE — it
    // can no longer play the wrong song, because the index travels with the track it belongs to.
    val upcoming =
        remember(state.artworkQueue, state.currentOrderIndex) {
            if (state.currentOrderIndex < 0) emptyList() else state.artworkQueue.withIndex().drop(offset)
        }
    // Row keys by track, NOT by position: a radio queue drops played tracks off the front, and a
    // position-based key changes for every row when that happens, so the list loses its scroll
    // anchor and jumps under the user. The occurrence number keeps it unique when a radio repeats
    // a song. Indexed by the ABSOLUTE queue position each row carries.
    val rowKeys =
        remember(state.artworkQueue) {
            val seen = HashMap<String, Int>(state.artworkQueue.size)
            state.artworkQueue.map { track ->
                val occurrence = seen.getOrElse(track.videoId) { 0 }
                seen[track.videoId] = occurrence + 1
                "${track.videoId}#$occurrence"
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        // statusBars + 20dp, not a bare status-bar offset: the grabber that
        // NowPlayingContentImmersive draws above the view Crossfade floats over this column, and
        // without the extra room the header's title slides underneath it.
        Spacer(
            modifier =
                Modifier.height(
                    with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() } + 20.dp,
                ),
        )
        ImmersiveCompactHeader(state = state, actions = actions, typography = typography)
        // A Listen Together guest follows the host's queue and can't reorder or take songs out of it.
        val canEdit = !musicServiceHandler.followingRoom
        // Unlocked: the rows show their drag handles and a long press drags too.
        var reordering by rememberSaveable { mutableStateOf(false) }
        ImmersiveQueuePillsRow(
            state = state,
            actions = actions,
            reordering = reordering.takeIf { canEdit },
            onReorderingChange = { reordering = it },
            activePillContainer = activePillContainer,
            activePillContent = activePillContent,
            modifier = Modifier.padding(top = 4.dp, bottom = if (state.isShort) 8.dp else 20.dp),
        )
        ImmersiveContinuePlayingHeader(
            state = state,
            dataStoreManager = dataStoreManager,
            typography = typography,
            activePillContainer = activePillContainer,
            activePillContent = activePillContent,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        val lazyListState = rememberLazyListState()
        // The offset at the drop, not at the first frame: the track playing may have changed since.
        val currentOffset by rememberUpdatedState(offset)
        // The list is upcoming tracks only, so its first row is the one right after the playing
        // song (shown in the header above): a row stops there, and pulling it further says why.
        val belowPlaying = stringResource(Res.string.queue_move_below_playing)
        val dragDropState =
            rememberDragDropState(
                lazyListState,
                canMoveTo = { canEdit && it in upcoming.indices },
                onBlocked = { showToast(belowPlaying, ToastGravity.Bottom) },
            ) { from, to ->
                actions.onMoveQueueItem(from + currentOffset, to + currentOffset)
            }
        val rows = dragDropState.arrange(upcoming)

        // Follow the track change. The list holds only what is still to come, so every time the
        // player advances the row that was at the top leaves it and everything shifts up by one —
        // but the scroll offset does not move, so a queue the user had scrolled through stays
        // parked mid-list with the track that is actually next off-screen. Re-anchoring to the top
        // is what "showing the current position" means for an upcoming-only list.
        LaunchedEffect(state.currentOrderIndex) {
            if (state.currentOrderIndex >= 0) {
                lazyListState.animateScrollToItem(0)
            }
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .immersiveVerticalFadeEdges(topFade = QUEUE_TOP_FADE, bottomFade = QUEUE_BOTTOM_FADE),
        ) {
            QuickLongPressBox(
                gesture =
                    Modifier.pointerInput(canEdit, reordering) {
                        // Same guards as QueueBottomSheet: locked, or a room's follower, nothing moves.
                        if (!canEdit || !reordering) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragOffset ->
                                change.consume()
                                // Also scrolls the list while the row is held near its top or bottom.
                                dragDropState.onDrag(dragOffset)
                            },
                            onDragStart = { dragStartOffset -> dragDropState.onDragStart(dragStartOffset) },
                            onDragEnd = { dragDropState.onDragInterrupted(true) },
                            onDragCancel = { dragDropState.onDragInterrupted() },
                        )
                    },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = lazyListState,
                    // Space at BOTH ends equal to the fade at that end, so each fade lands on blank
                    // space instead of dissolving a real row. Only the bottom had it, which is why the
                    // first upcoming track — the one you most want to read — came up half faded out.
                    contentPadding = PaddingValues(top = QUEUE_TOP_FADE, bottom = QUEUE_BOTTOM_FADE),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        rows,
                        // Absolute index in the key: `upcoming` is a sublist, so a bare local index
                        // shifts on every track change and invalidates every row.
                        key = { _, item -> rowKeys.getOrElse(item.index) { item.value.videoId } },
                    ) { localIndex, item ->
                        val track = item.value
                        // The queue-wide index this row actually has. Everything the PLAYER is told
                        // uses this; only the drag gesture below uses localIndex, because that one is
                        // genuinely about position within the visible list.
                        val queueIndex = item.index
                        DraggableItem(
                            dragDropState = dragDropState,
                            index = localIndex,
                            key = rowKeys.getOrElse(queueIndex) { track.videoId },
                            modifier = Modifier,
                            // A white veil, like the rows' white text: the blurred cover stays visible through it.
                            liftColor = translucentLift(Color.White),
                        ) { _ ->
                            // A leftward swipe takes it off the queue, like the queue sheet's rows: these
                            // are all upcoming, so only a guest (or a reorder under way) can't.
                            SwipeToRemoveQueueRow(
                                index = queueIndex,
                                videoId = track.videoId,
                                enabled = canEdit && dragDropState.currentIndexOfDraggedItem == null,
                                onRemove = { actions.onRemoveQueueItem(queueIndex, track.videoId) },
                            ) {
                                // Owner's call: the OLD queue sheet's row component, verbatim — no bespoke
                                // row. Its handle, or a long press (list-level gesture above), reorders; ⋯ opens the
                                // full song sheet with the queue's own rows, hosted by the player screen.
                                SongFullWidthItems(
                                    track = track,
                                    isPlaying = false,
                                    suggested = track.suggested,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClickListener = { videoId ->
                                        if (videoId == track.videoId) actions.onSeekToQueueIndex(queueIndex)
                                    },
                                    onMoreClickListener = {
                                        actions.onQueueItemMore(queueIndex, track)
                                    },
                                    trailingView =
                                        if (canEdit) {
                                            {
                                                val up = { actions.onMoveQueueItem(queueIndex, queueIndex - 1) }
                                                val down = { actions.onMoveQueueItem(queueIndex, queueIndex + 1) }
                                                ReorderHandle(
                                                    dragDropState,
                                                    index = localIndex,
                                                    onMoveUp = up.takeIf { localIndex > 0 },
                                                    onMoveDown = down.takeIf { localIndex < rows.lastIndex },
                                                    visible = reordering,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }

        ImmersiveBottomCluster(
            state = state,
            actions = actions,
            typography = typography,
            viewState = viewState,
            onSelectView = onSelectView,
            activePillContainer = activePillContainer,
            activePillContent = activePillContent,
            deviceVolumeController = deviceVolumeController,
            outputName = outputName,
            onOpenOutput = onOpenOutput,
        )
    }
}

/**
 * [LimusicIcons.Info] [LimusicIcons.PlaylistAdd] [LimusicIcons.Shuffle] [LimusicIcons.Repeat], then
 * the queue's lock (open while [reordering]) unless [reordering] is null (a guest).
 */
@Composable
private fun ImmersiveQueuePillsRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    reordering: Boolean?,
    onReorderingChange: (Boolean) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    modifier: Modifier = Modifier,
) {
    val repeatState = state.controllerState.repeatState
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ImmersiveQueuePill(
            icon = LimusicIcons.Info,
            active = false,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onShowInfo() },
            modifier = Modifier.weight(1f),
        )
        ImmersiveQueuePill(
            icon = LimusicIcons.PlaylistAdd,
            active = false,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onShowAddToPlaylist() },
            modifier = Modifier.weight(1f),
        )
        ImmersiveQueuePill(
            icon = state.controllerState.shuffleIcon,
            active = state.controllerState.isShuffle,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onUIEvent(UIEvent.Shuffle) },
            modifier = Modifier.weight(1f),
        )
        ImmersiveQueuePill(
            icon = if (repeatState is RepeatState.One) LimusicIcons.RepeatOne else LimusicIcons.Repeat,
            active = repeatState !is RepeatState.None,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { actions.onUIEvent(UIEvent.Repeat) },
            modifier = Modifier.weight(1f),
        )
        if (reordering != null) {
            ImmersiveQueuePill(
                icon = LimusicIcons.Lock,
                active = reordering,
                activeContainer = activePillContainer,
                activeContent = activePillContent,
                onClick = { onReorderingChange(!reordering) },
                modifier = Modifier.weight(1f),
                iconContent = { tint ->
                    QueueLockIcon(
                        open = reordering,
                        contentDescription = stringResource(Res.string.drag_to_reorder),
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ImmersiveQueuePill(
    icon: ImageVector,
    active: Boolean,
    activeContainer: Color,
    activeContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Drawn instead of [icon] when given, in the pill's icon colour.
    iconContent: (@Composable (tint: Color) -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                // Gentler than the default 1.35×: a pill is 83dp wide with only a 10dp gap, so
                // the full inflate would visibly overlap its neighbour on every tap.
                .immersivePressInflate(pressedScale = 1.08f)
                .height(40.dp)
                .clip(rounded(20.dp))
                .background(if (active) activeContainer else ImmersivePillInactive)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (active) activeContent else Color.White
        if (iconContent != null) {
            iconContent(tint)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "",
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ImmersiveContinuePlayingHeader(
    state: NowPlayingContentState,
    dataStoreManager: DataStoreManager,
    typography: ImmersiveTypography,
    activePillContainer: Color,
    activePillContent: Color,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    // remember the mapped flow: a bare .map in composition builds a new Flow every recomposition
    // (FlowOperatorInvokedInComposition, promoted to an error here).
    val endlessQueueFlow =
        remember(dataStoreManager) {
            dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }
        }
    val endlessQueueEnabled by endlessQueueFlow.collectAsStateWithLifecycle(initialValue = false)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Two stacked lines instead of one "Continue Playing": the small label says WHAT this
            // section is, the line under it says WHERE the queue came from. A queue with no source
            // name (a bare radio, a restored session) simply drops the second line rather than
            // printing an empty one.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.now_playing),
                    style = typography.queueSectionSubtitle,
                )
                val source = state.screenData.playlistName
                if (source.isNotBlank()) {
                    Text(
                        text = source,
                        style = typography.queueSectionHeader,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // The switch needs its own label, exactly like the queue sheet's — unlabelled it
            // reads as a mystery toggle.
            Text(
                text = stringResource(Res.string.endless_queue),
                style = typography.queueSectionSubtitle,
                modifier = Modifier.padding(end = 8.dp),
            )
            Switch(
                checked = endlessQueueEnabled,
                onCheckedChange = { checked ->
                    coroutineScope.launch { dataStoreManager.setEndlessQueue(checked) }
                },
                colors =
                    SwitchDefaults.colors(
                        // On state takes the artwork-derived pair this style already uses for its
                        // active pills, instead of the theme's green — on a page painted from the
                        // cover art, a fixed accent is the one element that does not belong to the
                        // record playing.
                        checkedTrackColor = activePillContainer,
                        checkedThumbColor = activePillContent,
                        checkedBorderColor = Color.Transparent,
                        // Transparent when off, so the control reads as an outline sitting on the
                        // page rather than a grey slab: this row has no surface of its own, and
                        // Material's default unchecked track paints one.
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = Color.White.copy(alpha = 0.45f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                    ),
                modifier = Modifier.immersivePressInflate(pressedScale = 1.08f),
            )
        }
    }
}

// The fade at each edge of the queue list, and the content padding that matches it — declared
// once so the two can never drift apart.
private val QUEUE_TOP_FADE = 24.dp
private val QUEUE_BOTTOM_FADE = 48.dp
