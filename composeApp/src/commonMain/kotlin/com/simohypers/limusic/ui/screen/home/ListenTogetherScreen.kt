package com.simohypers.limusic.ui.screen.home

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import com.simohypers.limusic.expect.ui.KeepScreenBright
import com.simohypers.limusic.expect.ui.encodeQr
import com.simohypers.limusic.ui.component.RoomQrCode
import com.simohypers.limusic.ui.component.RoomQrScanner
import com.simohypers.limusic.ui.component.qrInks
import com.simohypers.limusic.ui.icon.QrCode
import com.simohypers.limusic.ui.icon.QrCodeScanner
import com.simohypers.limusic.viewModel.SharedViewModel
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.ltd_err_name_scan
import limusic.composeapp.generated.resources.ltd_hide_qr
import limusic.composeapp.generated.resources.ltd_in_another_room
import limusic.composeapp.generated.resources.ltd_invite_server
import limusic.composeapp.generated.resources.ltd_qr_description
import limusic.composeapp.generated.resources.ltd_qr_hint
import limusic.composeapp.generated.resources.ltd_scan
import limusic.composeapp.generated.resources.ltd_show_qr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.domain.data.model.listentogether.ListenTogetherRoom
import com.simohypers.limusic.domain.data.model.listentogether.RoomConnection
import com.simohypers.limusic.domain.data.model.listentogether.RoomMember
import com.simohypers.limusic.expect.shareUrl
import com.simohypers.limusic.ui.component.CodeInput
import com.simohypers.limusic.ui.component.LimusicLoader
import com.simohypers.limusic.ui.component.SettingSheet
import com.simohypers.limusic.ui.component.codeOf
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.ArrowForwardIos
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.ContentCopy
import com.simohypers.limusic.ui.icon.Edit
import com.simohypers.limusic.ui.icon.Groups
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.PersonAdd
import com.simohypers.limusic.ui.icon.Share
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Star
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.devices_paused
import limusic.composeapp.generated.resources.devices_playing
import limusic.composeapp.generated.resources.ltd_alone_hint
import limusic.composeapp.generated.resources.ltd_approve
import limusic.composeapp.generated.resources.ltd_auto_admit
import limusic.composeapp.generated.resources.ltd_auto_admit_hint
import limusic.composeapp.generated.resources.ltd_connecting
import limusic.composeapp.generated.resources.ltd_copied
import limusic.composeapp.generated.resources.ltd_copy
import limusic.composeapp.generated.resources.ltd_decline
import limusic.composeapp.generated.resources.ltd_err_enter_name
import limusic.composeapp.generated.resources.ltd_err_paste_code
import limusic.composeapp.generated.resources.ltd_host
import limusic.composeapp.generated.resources.ltd_hosting
import limusic.composeapp.generated.resources.ltd_join
import limusic.composeapp.generated.resources.ltd_join_requests
import limusic.composeapp.generated.resources.ltd_join_room
import limusic.composeapp.generated.resources.ltd_join_room_hint
import limusic.composeapp.generated.resources.ltd_leave_hint_alone
import limusic.composeapp.generated.resources.ltd_leave_hint_guest
import limusic.composeapp.generated.resources.ltd_leave_hint_host
import limusic.composeapp.generated.resources.ltd_leave_room
import limusic.composeapp.generated.resources.ltd_listening
import limusic.composeapp.generated.resources.ltd_make_host
import limusic.composeapp.generated.resources.ltd_make_host_hint
import limusic.composeapp.generated.resources.ltd_offline
import limusic.composeapp.generated.resources.ltd_people
import limusic.composeapp.generated.resources.ltd_remove_from_room
import limusic.composeapp.generated.resources.ltd_remove_hint
import limusic.composeapp.generated.resources.ltd_resync
import limusic.composeapp.generated.resources.ltd_resync_hint
import limusic.composeapp.generated.resources.ltd_share_invite
import limusic.composeapp.generated.resources.ltd_share_text
import limusic.composeapp.generated.resources.ltd_start_room
import limusic.composeapp.generated.resources.ltd_start_room_hint
import limusic.composeapp.generated.resources.ltd_status_connected
import limusic.composeapp.generated.resources.ltd_status_connecting
import limusic.composeapp.generated.resources.ltd_status_disconnected
import limusic.composeapp.generated.resources.ltd_subtitle
import limusic.composeapp.generated.resources.ltd_sync_server
import limusic.composeapp.generated.resources.ltd_sync_server_hint
import limusic.composeapp.generated.resources.ltd_sync_server_placeholder
import limusic.composeapp.generated.resources.ltd_title
import limusic.composeapp.generated.resources.ltd_use_default
import limusic.composeapp.generated.resources.ltd_waiting_for_host
import limusic.composeapp.generated.resources.ltd_you
import limusic.composeapp.generated.resources.ltd_your_name
import limusic.composeapp.generated.resources.now_playing
import limusic.composeapp.generated.resources.save

/**
 * Listen Together, built like the Devices page: the start page (your name, Start a room, Join a
 * room), the wait for the host, and the room (its code, what plays, the people, Leave). The top
 * bar's ⋮ holds the sync server before a room, and a guest's Re-sync in one.
 */
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requesting by viewModel.requesting.collectAsStateWithLifecycle()
    val waiting = !state.inRoom && (requesting || state.pendingJoinCode != null)
    var menu by rememberSaveable { mutableStateOf(false) }
    var invite by rememberSaveable { mutableStateOf("") }

    // A limusic://listen link, most often a room's QR code read by the camera app: its code goes in
    // the Join box and waits for a tap. Already in a room, it only says so.
    val linkInvite by sharedViewModel.listenInviteRequest.collectAsStateWithLifecycle()
    LaunchedEffect(linkInvite) {
        val link = linkInvite ?: return@LaunchedEffect
        sharedViewModel.requestListenInvite(null)
        val code = codeOf(link, ListenTogetherViewModel.ROOM_CODE_LENGTH)
        when {
            code == (state.roomCode ?: state.pendingJoinCode) -> Unit
            state.inRoom || waiting -> sharedViewModel.makeToast(getString(Res.string.ltd_in_another_room))
            else -> {
                invite = link
                viewModel.clearError()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        SettingTopBar(
            title = stringResource(Res.string.ltd_title),
            onBack = { navController.navigateUp() },
            windowInsets = WindowInsets(0),
            actions = {
                if (!waiting && !state.isHost) {
                    IconButton(onClick = { menu = true }) {
                        Icon(LimusicIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
        Column(Modifier.padding(horizontal = 16.dp).animateContentSize()) {
            when {
                waiting -> Waiting(state.connection, viewModel::cancel)
                !state.inRoom -> Start(state, viewModel, invite) { invite = it }
                else -> Room(state, viewModel)
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }

    if (menu) {
        val close = { menu = false }
        if (state.inRoom) {
            SettingSheet(title = stringResource(Res.string.ltd_title), onDismiss = close) { colors ->
                SheetRow(stringResource(Res.string.ltd_resync), stringResource(Res.string.ltd_resync_hint), colors.content, colors.subtitle) {
                    viewModel.resync()
                    close()
                }
            }
        } else {
            ServerSheet(viewModel, close)
        }
    }
}

@Composable
private fun Waiting(
    connection: RoomConnection,
    onCancel: () -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    SettingCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LimusicLoader(Modifier.size(40.dp))
            Text(
                stringResource(if (connection is RoomConnection.Connected) Res.string.ltd_waiting_for_host else Res.string.ltd_connecting),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onCancel) { Text(stringResource(Res.string.cancel), color = LocalContentColor.current) }
        }
    }
}

/** Not in a room: who you are, then start one or join one (typed, or scanned from a QR code). */
@Composable
private fun Start(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
    invite: String,
    onInvite: (String) -> Unit,
) {
    val savedName by viewModel.savedName.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var editName by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<StringResource?>(null) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var cameraAt by remember { mutableStateOf<Offset?>(null) }
    val nameFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val nudge = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(savedName) { savedName?.takeIf { it.isNotEmpty() }?.let { name = it } }
    val nameMissing = localError == Res.string.ltd_err_enter_name || localError == Res.string.ltd_err_name_scan

    val go: (hosting: Boolean, code: String) -> Unit = { hosting, code ->
        localError = null
        viewModel.clearError()
        val n = name.trim()
        when {
            n.isEmpty() -> localError = Res.string.ltd_err_enter_name
            hosting -> viewModel.host(n)
            !viewModel.join(n, code) -> localError = Res.string.ltd_err_paste_code
        }
    }
    // The host sees the name in the join request, so the camera waits for one: the name card shakes
    // and, the first time, takes the keyboard.
    val scan: () -> Unit = {
        localError = null
        viewModel.clearError()
        if (name.isBlank()) {
            localError = Res.string.ltd_err_name_scan
            scope.launch {
                nudge.snapTo(0f)
                nudge.animateTo(1f, tween(460, easing = LinearEasing))
            }
            if (savedName == "") {
                runCatching { nameFocus.requestFocus() }
                keyboard?.show()
            }
        } else {
            keyboard?.hide()
            scanning = true
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HeaderIcon(LimusicIcons.Groups)
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(Res.string.ltd_subtitle),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The name is typed here once; after that it is a row, edited in a sheet.
        val shake = Modifier.graphicsLayer { translationX = shakeAt(nudge.value) * 10.dp.toPx() }
        when (savedName) {
            null -> Unit
            "" ->
                Box(shake) {
                    SettingCard {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(Res.string.ltd_your_name), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(10.dp))
                            NameField(
                                name,
                                error = localError.takeIf { nameMissing }?.let { stringResource(it) },
                                modifier = Modifier.focusRequester(nameFocus),
                            ) {
                                name = it
                                if (nameMissing) localError = null
                            }
                        }
                    }
                }
            else ->
                SettingCard {
                    Row(
                        Modifier.fillMaxWidth().clickable { editName = true }.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.ltd_your_name), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(name, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { editName = true }) {
                            Icon(LimusicIcons.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
        }

        SettingCard {
            Row(
                Modifier.fillMaxWidth().clickable { go(true, invite) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(LimusicIcons.Add)
                Spacer(Modifier.width(12.dp))
                TitleAndLine(stringResource(Res.string.ltd_start_room), stringResource(Res.string.ltd_start_room_hint), Modifier.weight(1f))
                Icon(LimusicIcons.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }

        SettingCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(LimusicIcons.PersonAdd)
                    Spacer(Modifier.width(12.dp))
                    TitleAndLine(stringResource(Res.string.ltd_join_room), stringResource(Res.string.ltd_join_room_hint), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                CodeInput(invite, length = ROOM_CODE_LENGTH, onInviteChange = onInvite)
                // A pasted or linked invite for someone's own server: say which, before Join goes there.
                if ('@' in invite) {
                    Text(
                        stringResource(Res.string.ltd_invite_server, invite.substringAfterLast('@')),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { go(false, invite) },
                        enabled = codeOf(invite, ROOM_CODE_LENGTH).length == ROOM_CODE_LENGTH,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(Res.string.ltd_join), color = LocalContentColor.current) }
                    Spacer(Modifier.width(8.dp))
                    ScanButton(
                        ready = name.isNotBlank(),
                        onClick = scan,
                        modifier =
                            Modifier.onGloballyPositioned {
                                cameraAt = it.positionOnScreen() + Offset(it.size.width / 2f, it.size.height / 2f)
                            },
                    )
                }
            }
        }
    }

    // A missing name shows under the name field when there is one.
    (localError?.takeUnless { nameMissing && savedName == "" }?.let { stringResource(it) } ?: state.error)?.let {
        Text(it, style = typo().bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 10.dp))
    }

    if (scanning) {
        RoomQrScanner(
            name = name.trim(),
            origin = cameraAt,
            onRoom = { scanned ->
                scanning = false
                onInvite(scanned)
                // A code on someone's own server waits for Join, with "On host" under the boxes:
                // joining moves this phone onto that server, and a printed code can point anywhere.
                if (ListenTogetherViewModel.serverOf(scanned).let { it == "" || it == serverUrl }) go(false, scanned)
            },
            onDismiss = { scanning = false },
        )
    }

    if (editName) {
        var draft by rememberSaveable { mutableStateOf(name) }
        val close = { editName = false }
        SettingSheet(title = stringResource(Res.string.ltd_your_name), onDismiss = close) {
            NameField(draft) { draft = it }
            Button(
                onClick = {
                    name = draft.trim()
                    viewModel.saveName(name)
                    close()
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(stringResource(Res.string.save), color = LocalContentColor.current) }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    error: String? = null,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(ListenTogetherViewModel.MAX_NAME_LENGTH)) },
        singleLine = true,
        placeholder = { Text(stringResource(Res.string.ltd_your_name)) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = rounded(12.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Opens the QR scanner. Muted until there is a name to join with, but it still answers a tap. */
@Composable
private fun ScanButton(
    ready: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (ready) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
    )
    val content by animateColorAsState(
        if (ready) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    )
    val label = stringResource(Res.string.ltd_scan)
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(LimusicIcons.QrCodeScanner, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

/** A head shake: three swings that die down as [t] runs from 0 to 1, still at 1. */
private fun shakeAt(t: Float): Float = sin(t * 6f * PI.toFloat()) * (1f - t)

/** In a room: the code to hand out, what plays, who is here, and Leave. */
@Composable
private fun Room(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
) {
    var managing by remember { mutableStateOf<RoomMember?>(null) }
    val others = state.members.filter { it.userId != state.selfUserId }

    Spacer(Modifier.height(8.dp))
    CodeCard(state, viewModel, alone = state.isHost && others.isEmpty() && state.joinRequests.isEmpty())

    if (state.isHost && state.joinRequests.isNotEmpty()) {
        SettingLabel(stringResource(Res.string.ltd_join_requests))
        SettingCard {
            Column(Modifier.padding(vertical = 6.dp)) {
                state.joinRequests.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(r.username, highlight = true)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            r.username,
                            style = typo().labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.rejectJoin(r.userId) }) {
                            Text(stringResource(Res.string.ltd_decline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { viewModel.approveJoin(r.userId) }, contentPadding = PaddingValues(horizontal = 16.dp)) {
                            Text(stringResource(Res.string.ltd_approve), color = LocalContentColor.current)
                        }
                    }
                }
            }
        }
    }

    state.currentTrack?.let { track ->
        SettingLabel(stringResource(Res.string.now_playing))
        SettingCard {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = track.thumbnail.ifBlank { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(rounded(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.width(12.dp))
                TitleAndLine(
                    track.title,
                    listOf(track.artist, stringResource(if (state.isPlaying) Res.string.devices_playing else Res.string.devices_paused))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    Modifier.weight(1f),
                )
            }
        }
    }

    SettingLabel(stringResource(Res.string.ltd_people, state.members.size))
    SettingCard {
        Column(Modifier.padding(vertical = 6.dp)) {
            state.members.forEach { u ->
                Person(
                    u,
                    isSelf = u.userId == state.selfUserId,
                    onManage = { managing = u }.takeIf { state.isHost && u.userId != state.selfUserId },
                )
            }
        }
    }

    if (state.isHost) {
        val autoAdmit by viewModel.autoAdmit.collectAsStateWithLifecycle()
        Spacer(Modifier.height(12.dp))
        SettingCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = autoAdmit, role = Role.Switch, onValueChange = viewModel::setAutoAdmit)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TitleAndLine(stringResource(Res.string.ltd_auto_admit), stringResource(Res.string.ltd_auto_admit_hint), Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Switch(checked = autoAdmit, onCheckedChange = null)
            }
        }
    }

    // Leaving, as Devices puts Unlink: a quiet row at the bottom, not a red button.
    Spacer(Modifier.height(28.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(rounded(20.dp))
            .clickable(onClick = viewModel::leave)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(Res.string.ltd_leave_room), style = typo().labelMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(2.dp))
        val host = state.members.firstOrNull { it.isHost }?.username.orEmpty()
        Text(
            when {
                !state.isHost -> stringResource(Res.string.ltd_leave_hint_guest, host)
                others.any { it.isConnected } -> stringResource(Res.string.ltd_leave_hint_host)
                else -> stringResource(Res.string.ltd_leave_hint_alone)
            },
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    managing?.let { u ->
        val close = { managing = null }
        SettingSheet(title = u.username, onDismiss = close) { colors ->
            SheetRow(stringResource(Res.string.ltd_make_host), stringResource(Res.string.ltd_make_host_hint, u.username), colors.content, colors.subtitle) {
                viewModel.transferHost(u.userId)
                close()
            }
            SheetRow(stringResource(Res.string.ltd_remove_from_room), stringResource(Res.string.ltd_remove_hint, u.username), MaterialTheme.colorScheme.error, colors.subtitle) {
                viewModel.kick(u.userId)
                close()
            }
        }
    }
}

/** The room's code, big enough to read out, with Share and Copy under it. */
@Composable
private fun CodeCard(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
    alone: Boolean,
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val code = state.roomCode.orEmpty()
    val invite = ListenTogetherViewModel.makeInvite(serverUrl, code)
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val shareTitle = stringResource(Res.string.ltd_title)
    val shareText = stringResource(Res.string.ltd_share_text, invite)

    SettingCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val connected = state.connection is RoomConnection.Connected
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.width(6.dp))
                val status =
                    when (state.connection) {
                        is RoomConnection.Connected -> Res.string.ltd_status_connected
                        RoomConnection.Connecting -> Res.string.ltd_status_connecting
                        else -> Res.string.ltd_status_disconnected
                    }
                Text(
                    stringResource(status) + " · " + stringResource(if (state.isHost) Res.string.ltd_hosting else Res.string.ltd_listening),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            // In two halves, the way it gets read out and typed.
            Text(
                code.chunked(4).joinToString(" "),
                style = typo().headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (serverUrl.isNotEmpty()) {
                Text(
                    invite,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { shareUrl(shareTitle, shareText) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(LimusicIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.ltd_share_invite), color = LocalContentColor.current)
                }
                FilledTonalButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(invite))
                        copied = true
                        scope.launch {
                            delay(1600)
                            copied = false
                        }
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(if (copied) LimusicIcons.Check else LimusicIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (copied) Res.string.ltd_copied else Res.string.ltd_copy), color = LocalContentColor.current)
                }
            }
            Spacer(Modifier.height(8.dp))
            QrToggle(
                link = ListenTogetherViewModel.makeLink(serverUrl, code),
                code = code,
                live = state.connection is RoomConnection.Connected,
            )
            if (alone) {
                Text(
                    stringResource(Res.string.ltd_alone_hint),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/**
 * "Show QR code", which grows in place into the room's QR code on a white tile: the button stretches
 * into a square, then the code builds itself. Tapping the tile or Hide folds it back. While it
 * shows, the screen is at full brightness; while the connection is down the code dims behind a
 * loader, since a scan then finds nobody to let the guest in.
 */
@Composable
private fun QrToggle(
    link: String,
    code: String,
    live: Boolean,
) {
    val matrix = remember(link) { encodeQr(link) } ?: return
    var open by remember { mutableStateOf(false) }
    val morph = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (open) {
            launch { morph.animateTo(1f, spring(dampingRatio = 1f, stiffness = 320f)) }
            delay(180)
            reveal.animateTo(1f, tween(650, easing = LinearEasing))
        } else {
            launch { reveal.animateTo(0f, tween(160)) }
            delay(70)
            morph.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f))
        }
    }
    if (open) KeepScreenBright()

    val (ink, eyeInk) = qrInks(MaterialTheme.colorScheme.primary)
    val button = MaterialTheme.colorScheme.secondaryContainer
    val onButton = MaterialTheme.colorScheme.onSecondaryContainer
    val dim by animateFloatAsState(if (live) 1f else 0.2f)
    val labelShown by remember { derivedStateOf { morph.value < 0.34f } }
    val codeShown by remember { derivedStateOf { morph.value > 0f } }
    val toggleLabel = stringResource(if (open) Res.string.ltd_hide_qr else Res.string.ltd_show_qr)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val t = morph.value
                    val full = constraints.maxWidth
                    val square = min(full, QR_SIDE.roundToPx()).toFloat()
                    val w = lerp(full.toFloat(), square, t).roundToInt().coerceIn(0, full)
                    val h = lerp(48.dp.toPx(), square, t).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(full, h) { placeable.place((full - w) / 2, 0) }
                }.clip(rounded(24.dp))
                .drawBehind { drawRect(lerp(button, Color.White, morph.value.coerceIn(0f, 1f))) }
                .clickable(onClickLabel = toggleLabel, role = Role.Button) { open = !open },
        ) {
            if (labelShown) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .height(48.dp)
                        .graphicsLayer { alpha = (1f - morph.value * 3f).coerceIn(0f, 1f) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LimusicIcons.QrCode, contentDescription = null, tint = onButton, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.ltd_show_qr), style = MaterialTheme.typography.labelLarge, color = onButton)
                }
            }
            if (codeShown) {
                RoomQrCode(
                    matrix = matrix,
                    reveal = { reveal.value },
                    ink = ink,
                    eyeInk = eyeInk,
                    description = stringResource(Res.string.ltd_qr_description, code.chunked(4).joinToString(" ")),
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = dim },
                )
            }
            if (open && !live) LimusicLoader(Modifier.align(Alignment.Center).size(40.dp))
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(260, delayMillis = 300)) + expandVertically(tween(260)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(220)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(Res.string.ltd_qr_hint),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { open = false }) {
                    Text(stringResource(Res.string.ltd_hide_qr), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** The QR tile's side, or the card's width if that is less. */
private val QR_SIDE = 272.dp

@Composable
private fun Person(
    u: RoomMember,
    isSelf: Boolean,
    onManage: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = if (onManage != null) 4.dp else 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(u.username, highlight = u.isHost, modifier = Modifier.alpha(if (u.isConnected) 1f else 0.5f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(u.username, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!u.isConnected) {
                Text(stringResource(Res.string.ltd_offline), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val tags = listOfNotNull(stringResource(Res.string.ltd_host).takeIf { u.isHost }, stringResource(Res.string.ltd_you).takeIf { isSelf })
        if (tags.isNotEmpty()) {
            if (u.isHost) {
                Icon(LimusicIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                tags.joinToString(" · "),
                style = typo().bodySmall,
                color = if (u.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onManage != null) {
            IconButton(onClick = onManage) {
                Icon(LimusicIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The first letter in a circle, tinted for the host or someone asking in. */
@Composable
private fun Avatar(
    name: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = typo().labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoundIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TitleAndLine(
    title: String,
    line: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(line, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A sheet row as Devices draws its Unlink: a title and one line under it. */
@Composable
private fun SheetRow(
    title: String,
    line: String,
    titleColor: Color,
    lineColor: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(rounded(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(title, style = typo().labelMedium, color = titleColor)
        Spacer(Modifier.height(2.dp))
        Text(line, style = typo().bodySmall, color = lineColor)
    }
}

/** The sync server, for the few who run their own. Applies from the next room. */
@Composable
private fun ServerSheet(
    viewModel: ListenTogetherViewModel,
    onDismiss: () -> Unit,
) {
    val saved by viewModel.serverUrl.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf(saved.ifEmpty { ListenTogetherViewModel.DEFAULT_SERVER_URL }) }
    SettingSheet(title = stringResource(Res.string.ltd_sync_server), onDismiss = onDismiss) { colors ->
        Text(stringResource(Res.string.ltd_sync_server_hint), style = typo().bodySmall, color = colors.subtitle)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.ltd_sync_server_placeholder)) },
            textStyle = typo().bodyMedium,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            shape = rounded(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (draft.trim() != ListenTogetherViewModel.DEFAULT_SERVER_URL) {
                TextButton(onClick = { draft = ListenTogetherViewModel.DEFAULT_SERVER_URL }) {
                    Text(stringResource(Res.string.ltd_use_default), color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.setServer(draft)
                    onDismiss()
                },
                enabled = draft.isNotBlank(),
            ) { Text(stringResource(Res.string.save), color = LocalContentColor.current) }
        }
    }
}

private const val ROOM_CODE_LENGTH = ListenTogetherViewModel.ROOM_CODE_LENGTH
