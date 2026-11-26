package com.simohypers.limusic.viewModel

import com.simohypers.limusic.viewModel.ListenTogetherViewModel.Companion.inviteOfLink
import com.simohypers.limusic.viewModel.ListenTogetherViewModel.Companion.makeLink
import com.simohypers.limusic.viewModel.ListenTogetherViewModel.Companion.parseInvite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomLinkTest {
    @Test
    fun defaultServerIsABareCode() {
        val link = makeLink("", "AB12CD34")
        assertEquals("limusic://listen?room=AB12CD34", link)
        assertEquals("AB12CD34", inviteOfLink(link))
    }

    @Test
    fun customServerRoundTripsToTheSameInvite() {
        for (server in listOf("wss://sync.example.org/ws", "ws://127.0.0.1:8099/ws", "wss://host:8443/custom/path")) {
            val link = makeLink(server, "QWERTY12")
            val invite = inviteOfLink(link)!!
            assertEquals(ListenTogetherViewModel.makeInvite(server, "QWERTY12"), invite)
            // And the invite still names the server it came from.
            assertEquals(server to "QWERTY12", parseInvite(invite))
        }
    }

    @Test
    fun theServerIsEscapedInTheLink() {
        assertEquals("limusic://listen?room=QWERTY12&server=ws%3A%2F%2F127.0.0.1%3A8099", makeLink("ws://127.0.0.1:8099/ws", "QWERTY12"))
        assertEquals("limusic://listen?room=QWERTY12&server=sync.example.org", makeLink("wss://sync.example.org/ws", "QWERTY12"))
    }

    @Test
    fun serverOfMatchesHowTheSavedServerIsStored() {
        assertEquals("", ListenTogetherViewModel.serverOf("AB12CD34"))
        assertEquals("", ListenTogetherViewModel.serverOf("AB12CD34@" + ListenTogetherViewModel.DEFAULT_SERVER_URL.removePrefix("wss://").removeSuffix("/ws")))
        assertEquals("wss://sync.example.org/ws", ListenTogetherViewModel.serverOf(inviteOfLink(makeLink("wss://sync.example.org/ws", "AB12CD34"))!!))
        assertNull(ListenTogetherViewModel.serverOf(" "))
    }

    @Test
    fun readsWhatACameraAppHandsOver() {
        assertEquals("AB12CD34", inviteOfLink("  LIMUSIC://LISTEN?room=ab12cd34\n"))
        assertEquals("AB12CD34", inviteOfLink("limusic://listen?server=&room=AB12CD34"))
    }

    @Test
    fun anythingElseIsNotARoom() {
        listOf(
            "",
            "AB12CD34",
            "https://example.com/?room=AB12CD34",
            "limusic://watch?v=dQw4w9WgXcQ",
            "limusic://listen",
            "limusic://listen?room=",
            "limusic://listen?room=AB12CD3",
            "limusic://listen?room=AB12CD345",
            "limusic://listen?room=AB12-D34",
            "limusic://listen?room=AB12CD34&server=evil%20host",
            "limusic://listen?room=AB12CD34&server=a%40b",
            "WIFI:S:home;T:WPA;P:secret;;",
        ).forEach { assertNull(inviteOfLink(it), it) }
    }
}
