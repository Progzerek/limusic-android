package com.simohypers.limusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eygraber.uri.Uri
import com.simohypers.limusic.data.listentogether.ListenTogetherPlaybackBridge
import com.simohypers.limusic.data.listentogether.ListenTogetherPrefs
import com.simohypers.limusic.domain.data.model.listentogether.ListenTogetherRoom
import com.simohypers.limusic.domain.data.model.listentogether.RoomConnection
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.repository.ListenTogetherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Listen Together, laid out and behaving like Limusic desktop's dialog: no separate connect step
 * (Start or Join connects first), the server lives in the Host tab, invites are `CODE@host` or a
 * bare code on the default server.
 */
class ListenTogetherViewModel(
    private val repository: ListenTogetherRepository,
    private val dataStore: DataStoreManager,
    bridge: ListenTogetherPlaybackBridge,
) : ViewModel() {
    // Above init: the collector there runs inside the constructor (Main.immediate, and a StateFlow
    // replays at once), so while in a room it writes this before anything declared below init
    // exists. Declared below, it was null there and the screen crashed.
    private val _requesting = MutableStateFlow(false)

    init {
        // The bridge is a singleton that outlives this screen; starting it here is simply the
        // first moment anything asks for it. Koin's `createdAtStart` does not fire for modules
        // added with loadKoinModules, so without an explicit injection it is never constructed
        // at all, and then a room syncs membership while no audio follows anyone.
        bridge.start()
        viewModelScope.launch {
            repository.room.collect {
                if (it.inRoom || it.error != null || it.connection is RoomConnection.Failed) _requesting.value = false
            }
        }
    }

    val state: StateFlow<ListenTogetherRoom> =
        repository.room.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.room.value)

    /** The custom server, or empty on the default one (stored that way, as on desktop). */
    val serverUrl: StateFlow<String> =
        dataStore
            .getString(ListenTogetherPrefs.SERVER_URL)
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** The name used last time; null until read, so the page doesn't flash the first-time field. */
    val savedName: StateFlow<String?> =
        dataStore
            .getString(KEY_NAME)
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Between pressing Start or Join and being in the room. */
    val requesting: StateFlow<Boolean> = _requesting.asStateFlow()

    /** On the server picked in the ⋮ sheet. */
    fun host(name: String) = start(name, server = null) { repository.createRoom(name) }

    fun saveName(name: String) = viewModelScope.launch { dataStore.putString(KEY_NAME, name) }

    /** Takes effect from the next room: the connection is dropped only while in none. */
    fun setServer(url: String) =
        viewModelScope.launch {
            val server = normalize(url)
            if (server == dataStore.getString(ListenTogetherPrefs.SERVER_URL).first().orEmpty()) return@launch
            dataStore.putString(ListenTogetherPrefs.SERVER_URL, server)
            if (!repository.room.value.inRoom) repository.disconnect()
        }

    /** False when [invite] holds no room code. */
    fun join(
        name: String,
        invite: String,
    ): Boolean {
        val (server, code) = parseInvite(invite) ?: return false
        if (code.isEmpty()) return false
        // A bare code is what a host on the default server sends, so it means the default.
        start(name, normalize(server)) { repository.joinRoom(code, name) }
        return true
    }

    /** [server] null keeps the saved one. */
    private fun start(
        name: String,
        server: String?,
        send: () -> Unit,
    ) = viewModelScope.launch {
        _requesting.value = true
        dataStore.putString(KEY_NAME, name)
        if (server != null && server != dataStore.getString(ListenTogetherPrefs.SERVER_URL).first().orEmpty()) {
            dataStore.putString(ListenTogetherPrefs.SERVER_URL, server)
            repository.disconnect()
        }
        if (!repository.room.value.isConnected) {
            repository.connect()
            val connected =
                withTimeoutOrNull(CONNECT_TIMEOUT) {
                    repository.room.first { it.isConnected || it.connection is RoomConnection.Failed }
                }?.isConnected == true
            if (!connected) {
                _requesting.value = false
                return@launch
            }
        }
        send()
    }

    /** Gives up on a room not yet entered and closes the connection. */
    fun cancel() {
        _requesting.value = false
        repository.cancelJoin()
        repository.disconnect()
    }

    fun leave() = repository.leaveRoom()

    fun approveJoin(userId: String) = repository.approveJoin(userId)

    fun rejectJoin(userId: String) = repository.rejectJoin(userId)

    fun kick(userId: String) = repository.kickUser(userId)

    fun transferHost(userId: String) = repository.transferHost(userId)

    fun resync() = repository.requestSync()

    fun clearError() = repository.clearError()

    /** The default is stored as empty, so typing it in by hand still mints bare invites. */
    private fun normalize(url: String): String = url.trim().let { if (it == DEFAULT_SERVER_URL) "" else it }

    companion object {
        const val DEFAULT_SERVER_URL = ListenTogetherPrefs.DEFAULT_SERVER_URL
        private const val KEY_NAME = "lt_name"
        const val MAX_NAME_LENGTH = 24
        private val CONNECT_TIMEOUT = 15.seconds

        /** What a guest has to be sent: the bare code on the default server, else `CODE@host`. */
        fun makeInvite(
            server: String,
            code: String,
        ): String = if (server.isEmpty()) code else code + "@" + server.removePrefix("wss://").removeSuffix("/ws")

        /** (server, code) from an invite; server is empty for a bare code. Desktop's parser. */
        fun parseInvite(raw: String): Pair<String, String>? {
            val s = raw.trim()
            if (s.isEmpty()) return null
            val at = s.lastIndexOf('@')
            if (at < 0) return "" to s.uppercase()
            var server = s.substring(at + 1)
            if (!server.startsWith("wss://") && !server.startsWith("ws://")) server = "wss://$server"
            if ('/' !in server.substringAfter("://")) server += "/ws"
            return server to s.substring(0, at).uppercase()
        }

        /** The server an invite joins, stored the way the saved one is (empty for the default); null if it is no invite. */
        fun serverOf(invite: String): String? = parseInvite(invite)?.first?.let { if (it == DEFAULT_SERVER_URL) "" else it }

        /** The sync server's room codes: eight capital letters and digits. */
        const val ROOM_CODE_LENGTH = 8

        /** The deep link host a room's QR code points at. */
        const val LINK_HOST = "listen"

        /**
         * What a room's QR code holds: `limusic://listen?room=CODE`, plus `&server=` with the same
         * host part a `CODE@host` invite carries when the room is not on the default server. A
         * camera app opens it as a deep link, the in-app scanner reads it with [inviteOfLink].
         */
        fun makeLink(
            server: String,
            code: String,
        ): String {
            val link = "limusic://$LINK_HOST?room=$code"
            if (server.isEmpty()) return link
            return link + "&server=" + percentEncode(makeInvite(server, code).substringAfter('@'))
        }

        /**
         * The invite a room link stands for (`CODE`, or `CODE@host`), or null when [text] is
         * anything else: another app's link, a code of the wrong shape, a server that is not one.
         */
        fun inviteOfLink(text: String): String? {
            // Parsing never throws for a hierarchical link, but reading an odd one's query might.
            return runCatching {
                val uri = Uri.parse(text.trim())
                if (!uri.scheme.equals("limusic", ignoreCase = true) || !uri.host.equals(LINK_HOST, ignoreCase = true)) return null
                val code = uri.getQueryParameter("room")?.trim()?.uppercase() ?: return null
                if (code.length != ROOM_CODE_LENGTH || !code.all { it in 'A'..'Z' || it in '0'..'9' }) return null
                val server = uri.getQueryParameter("server")?.trim().orEmpty()
                if (server.any { it.isWhitespace() || it in "@?#" }) return null
                if (server.isEmpty()) code else "$code@$server"
            }.getOrNull()
        }

        /** Everything but RFC 3986's unreserved characters as %XX, so `ws://host:port/ws` survives a query. */
        private fun percentEncode(s: String): String =
            buildString {
                s.encodeToByteArray().forEach { b ->
                    val c = b.toInt().toChar()
                    if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in "-._~") {
                        append(c)
                    } else {
                        append('%').append(HEX[(b.toInt() shr 4) and 0xF]).append(HEX[b.toInt() and 0xF])
                    }
                }
            }

        private const val HEX = "0123456789ABCDEF"
    }
}
