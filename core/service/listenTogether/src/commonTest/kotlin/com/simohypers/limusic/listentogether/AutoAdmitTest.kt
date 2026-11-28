package com.simohypers.limusic.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AutoAdmitTest {
    private val host = ListenTogetherState(roomCode = "ABCD1234", isHost = true)

    @Test
    fun autoAdmitApprovesInsteadOfListing() {
        assertEquals(JoinAnswer.APPROVE, joinAnswer(host, "a", autoAdmit = true))

        // Off, the request waits for a tap, and a re-sent one is not listed twice.
        assertEquals(JoinAnswer.ASK, joinAnswer(host, "a", autoAdmit = false))
        val listed = host.copy(joinRequests = listOf(PendingJoin("a", "Ann")))
        assertEquals(JoinAnswer.KNOWN, joinAnswer(listed, "a", autoAdmit = false))
    }

    @Test
    fun aGuestNeverApproves() {
        val guest = ListenTogetherState(roomCode = "ABCD1234", isHost = false, joinRequests = listOf(PendingJoin("a", "Ann")))
        assertNotEquals(JoinAnswer.APPROVE, joinAnswer(guest, "b", autoAdmit = true))
        assertTrue(toAdmit(guest, on = true).isEmpty())
    }

    @Test
    fun turningItOnLetsInWhoIsWaiting() {
        val waiting = host.copy(joinRequests = listOf(PendingJoin("a", "Ann"), PendingJoin("b", "Bob")))
        assertEquals(listOf("a", "b"), toAdmit(waiting, on = true))
        assertTrue(toAdmit(waiting, on = false).isEmpty())
    }
}
