package com.simohypers.limusic.domain.mediaservice.queue

import com.simohypers.limusic.common.LOCAL_PLAYLIST_ID
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.mediaservice.handler.ShuffleMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Desktop's dynamic shuffle tests (src-tauri/src/state.rs), with the phone's pivot: the next track. */
class DynamicShuffleTest {
    private fun song(id: String) =
        Track(
            album = null,
            artists = null,
            duration = null,
            durationSeconds = null,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = null,
            title = id,
            videoId = id,
            videoType = null,
            category = null,
            feedbackTokens = null,
            resultType = null,
        )

    private fun sug(id: String) = song(id).copy(suggested = true)

    private fun joined(items: List<Track>) = items.joinToString(",") { it.videoId }

    private fun fresh(n: Int) = (0 until n).map { song("s$it") }

    /** "now" playing, then p0 to p(n-1). */
    private fun playlist(n: Int) = (listOf(song("now")) + (0 until n).map { song("p$it") }).toMutableList()

    private fun queue(
        items: List<Track>,
        current: Int = 0,
        orig: List<Track>? = null,
        dynamic: Boolean = false,
    ) = QueueState().apply {
        this.items = items.toMutableList()
        this.current = current
        shuffleOrig = orig?.toMutableList()
        this.dynamic = dynamic
    }

    // Two of the playlist's tracks, then a suggestion, and the playing track is the first of the
    // two. What doesn't fit is dropped.
    @Test
    fun suggestionsGoInEveryThirdTrack() {
        val items = playlist(9)
        assertEquals(5, interleaveSuggestions(items, 0, fresh(6), false))
        assertEquals("now,p0,s0,p1,p2,s1,p3,p4,s2,p5,p6,s3,p7,p8,s4", joined(items))
        assertEquals(5, items.count { it.suggested }, "each one is marked")
    }

    // Turned on mid-song with the next track already loading: the playing track and the next one
    // are the two, the third song is a suggestion.
    @Test
    fun thePlayingAndNextTracksCountTowardTheFirstSuggestion() {
        val items = playlist(6)
        interleaveSuggestions(items, 1, fresh(3), false)
        assertEquals("now,p0,s0,p1,p2,s1,p3,p4,s2,p5", joined(items))
        // A played suggestion resets the count: the next one waits for two more of the playlist's.
        val played = mutableListOf(song("p0"), sug("s0"), song("now"), song("p1"), song("p2"), song("p3"))
        interleaveSuggestions(played, 2, listOf(song("s1")), false)
        assertEquals("p0,s0,now,p1,s1,p2,p3", joined(played))
    }

    // A top-up carries on where the last one left off: rows already in place never move, and
    // nothing lands in front of the next track.
    @Test
    fun aTopUpCarriesOnAfterTheLastSuggestion() {
        val items = playlist(7)
        items.add(3, sug("s0"))
        assertEquals(2, interleaveSuggestions(items, 0, listOf(song("s1"), song("s2")), false))
        assertEquals("now,p0,p1,s0,p2,p3,s1,p4,p5,s2,p6", joined(items))

        val short = playlist(3)
        assertEquals(1, interleaveSuggestions(short, 1, listOf(song("s0")), false))
        assertEquals("now,p0,s0,p1,p2", joined(short), "right behind the next p0, not before it")
    }

    // A suggestion only follows a playlist track: never inside Play next, never among the adds
    // behind the playlist (unless the whole queue is one mix), never into autoplay.
    @Test
    fun suggestionsStayAmongThePlaylistsOwnTracks() {
        fun queue() =
            playlist(3).apply {
                add(1, song("q").copy(queued = true))
                for (x in listOf("x0", "x1", "x2")) add(song(x).copy(queuedEnd = true))
                add(song("a").copy(autoplay = true))
            }
        val items = queue()
        assertEquals(2, interleaveSuggestions(items, 0, fresh(3), false))
        assertEquals("now,q,p0,s0,p1,p2,s1,x0,x1,x2,a", joined(items))
        val whole = queue()
        assertEquals(3, interleaveSuggestions(whole, 0, fresh(3), true))
        assertEquals("now,q,p0,s0,p1,p2,s1,x0,x1,s2,x2,a", joined(whole))
    }

    // A count that comes due inside a Play next block waits for it: the suggestion goes after the
    // next playlist track, never between two rows the user lined up.
    @Test
    fun suggestionsNeverSplitAPlayNextBlock() {
        val items = mutableListOf(song("a"), song("now"), song("n1").copy(queued = true), song("n2").copy(queued = true))
        items.addAll((0 until 3).map { song("p$it") })
        interleaveSuggestions(items, 2, listOf(song("s0")), false)
        assertEquals("a,now,n1,n2,p0,s0,p1,p2", joined(items))
    }

    // A reshuffle (a playlist page arriving under shuffle) keeps them spread out, not clumped.
    @Test
    fun aReshuffleKeepsSuggestionsSpreadOut() {
        repeat(20) {
            val items = playlist(20)
            interleaveSuggestions(items, 0, fresh(6), false)
            shuffleUpcoming(items, 0, false)
            assertEquals(27, items.size)
            assertEquals(listOf(2, 5, 8, 11, 14, 17), items.indices.filter { items[it].suggested })
        }
    }

    // The track after the playing one is held: the player is already loading it.
    @Test
    fun theTrackAfterThePlayingOneIsNeverDisplaced() {
        val q = queue(playlist(4), orig = playlist(4), dynamic = true)
        assertEquals(1, q.absorbSuggestions(listOf(song("s0")), "now", whole = false))
        assertEquals("now,p0,s0,p1,p2,p3", joined(q.items))
    }

    @Test
    fun dynamicShuffleAsksForMoreOnlyWhenRunningLow() {
        val q = queue(playlist(12))
        assertNull(q.suggestionAnchor(false), "not on")
        q.dynamic = true
        assertEquals("now", q.suggestionAnchor(false), "none lined up yet")
        interleaveSuggestions(q.items, 0, fresh(4), false)
        assertNull(q.suggestionAnchor(false), "four ahead is enough")
        q.current = 3 // p1, past s0: three left, and room behind the last for more
        assertEquals("p1", q.suggestionAnchor(false))
        // Only one playlist track behind the last suggestion: nothing to put another after.
        val short = queue(playlist(3).apply { add(3, sug("s0")) }, dynamic = true)
        assertEquals("now,p0,p1,s0,p2", joined(short.items))
        assertNull(short.suggestionAnchor(false))
    }

    // The radio is mostly the playlist: what the queue or the snapshot holds goes. Nothing new
    // marks the anchor dry; a turn of the button clears it.
    @Test
    fun aPageWithNothingNewMarksTheAnchorDry() {
        val q = queue(playlist(6), orig = playlist(6) + song("later"), dynamic = true)
        assertEquals(0, q.absorbSuggestions(listOf(song("p2"), song("later")), "now", whole = false))
        assertEquals(q.epoch to "now", q.suggestDry)
        assertEquals(1, q.absorbSuggestions(listOf(song("p3"), song("new")), "now", whole = false))
        assertEquals(1, q.items.count { it.videoId == "new" && it.suggested })
        q.setShuffle(ShuffleMode.On, whole = false)
        q.setShuffle(ShuffleMode.Dynamic, whole = false)
        assertNull(q.suggestDry)
    }

    // The cycle: off, shuffle, dynamic, off. Off takes every suggestion with it but keeps the one
    // playing, where it is.
    @Test
    fun dynamicOffDropsTheSuggestionsButNotThePlayingOne() {
        val orig = (0 until 4).map { song("p$it") }
        val q =
            queue(
                listOf(song("p1"), sug("s0"), song("p3"), sug("s1"), song("p0"), song("p2")),
                current = 1,
                orig = orig,
                dynamic = true,
            )
        assertEquals(ShuffleMode.Dynamic, q.shuffleMode)
        assertTrue(q.setShuffle(ShuffleMode.Off, whole = false))
        assertEquals(ShuffleMode.Off, q.shuffleMode)
        // In behind p1, the playlist track that played before it, and the playlist carries on.
        assertEquals("p0,p1,s0,p2,p3", joined(q.items))
        assertEquals("s0", q.items[q.current].videoId, "the player's track is still the current row")
    }

    @Test
    fun dynamicBackToShuffleDropsOnlyUpcomingSuggestions() {
        val q = queue(listOf(sug("s0"), song("now"), song("p0"), sug("s1"), song("p1")), current = 1, orig = emptyList(), dynamic = true)
        assertTrue(q.setShuffle(ShuffleMode.On, whole = false))
        assertEquals("s0,now,p0,p1", joined(q.items))
        assertFalse(q.dynamic)
        assertFalse(q.setShuffle(ShuffleMode.On, whole = false), "already there")
    }

    // Playlists and albums only: a radio, a mix, a podcast or a playlist with no YouTube radio
    // behind it keeps the two-state button.
    @Test
    fun dynamicNeedsAPlaylistRadio() {
        fun q(source: String) =
            QueueState().apply {
                sourceId = source
                radioSeed = radioSeedFor(source)
            }
        assertTrue(q("VLPL123").dynamicAllowed())
        assertTrue(q("OLAK5uy_x").dynamicAllowed())
        assertFalse(q("VLRDCLAK5uy_x").dynamicAllowed(), "a mix")
        assertFalse(q("${LOCAL_PLAYLIST_ID}7").dynamicAllowed(), "no radio")
        assertFalse(q("MPSPPL123").dynamicAllowed(), "a podcast")
        assertFalse(q("VLPL123").apply { radio = true }.dynamicAllowed(), "a radio")
        assertFalse(QueueState().dynamicAllowed(), "one song")
    }

    @Test
    fun theButtonCyclesThroughDynamicOnlyWhereItsOnOffer() {
        val q = queue(playlist(5)).apply {
            sourceId = "VLPL1"
            radioSeed = radioSeedFor(sourceId)
        }
        q.toggleShuffle(whole = false)
        assertEquals(ShuffleMode.On, q.shuffleMode)
        q.toggleShuffle(whole = false)
        assertEquals(ShuffleMode.Dynamic, q.shuffleMode)
        q.toggleShuffle(whole = false)
        assertEquals(ShuffleMode.Off, q.shuffleMode)

        val mix = queue(playlist(5)).apply {
            sourceId = "RDCLAK5uy_x"
            radioSeed = radioSeedFor(sourceId)
        }
        mix.toggleShuffle(whole = false)
        mix.toggleShuffle(whole = false)
        assertEquals(ShuffleMode.Off, mix.shuffleMode)
    }

    // The page menu's pick: dynamic implies shuffle, a plain pick ends a sticky dynamic one, and a
    // sticky dynamic one carries into the next playlist but not into a mix.
    @Test
    fun startingAPageInAMode() {
        val tracks = (0 until 6).map { song("p$it") }
        val q = QueueState()
        q.startTracks(tracks, null, "VLPL1", "One", shuffle = false, sticky = true, whole = false, positionMs = 0, dynamic = true)
        assertEquals(ShuffleMode.Dynamic, q.shuffleMode)
        q.startTracks(tracks, null, "VLPL2", "Two", shuffle = false, sticky = true, whole = false, positionMs = 0)
        assertEquals(ShuffleMode.Dynamic, q.shuffleMode, "sticky")
        q.startTracks(tracks, null, "VLRDCLAK5uy_x", "Mix", shuffle = false, sticky = true, whole = false, positionMs = 0)
        assertEquals(ShuffleMode.On, q.shuffleMode, "nothing to draw on")
        q.startTracks(tracks, null, "VLPL3", "Three", shuffle = false, sticky = false, whole = false, positionMs = 0, dynamic = false)
        assertEquals(ShuffleMode.On, q.shuffleMode, "a plain pick still shuffles")
        q.dynamic = true
        q.startTracks(tracks, null, "VLPL4", "Four", shuffle = false, sticky = true, whole = false, positionMs = 0, dynamic = false)
        assertEquals(ShuffleMode.On, q.shuffleMode)
    }

    @Test
    fun previousBringsDynamicBack() {
        val q = queue(playlist(3), orig = playlist(3), dynamic = true)
        q.keepContext(0)
        assertFalse(q.dynamic)
        q.items = mutableListOf(song("x"))
        q.restoreContext(q.prev!!)
        assertTrue(q.dynamic)
    }

    // A song the user queues by hand is theirs: going back to plain shuffle leaves it be.
    @Test
    fun aSuggestionQueuedByHandIsNoLongerOne() {
        val q = queue(listOf(song("now"), song("p0"), sug("s0"), song("p1")), orig = emptyList(), dynamic = true)
        q.insertQueued(listOf(q.items[2]), next = true, from = null, dedupe = false, whole = false)
        assertEquals("now,s0,p0,s0,p1", joined(q.items))
        q.setShuffle(ShuffleMode.On, whole = false)
        assertEquals("now,s0,p0,p1", joined(q.items))
    }

    // A suggestion off a page the walk hadn't reached yet: when the page arrives the song is a
    // playlist track, and it is queued once. Heard, playing or next, it becomes the page's row in
    // place.
    @Test
    fun aLaterPlaylistPageTakesOverItsOwnSuggestions() {
        val items = playlist(4).apply {
            add(3, sug("x"))
            add(0, sug("y"))
        }
        val q = queue(items, orig = items.filter { !it.suggested }, dynamic = true)
        q.appendPage(listOf(song("x"), song("y"), song("p9")), playing = true, whole = false)
        fun count(id: String) = q.items.count { it.videoId == id }
        assertEquals(1, count("x"))
        assertTrue(q.items.none { it.suggested }, "both are playlist rows now")
        assertEquals("y", q.items[0].videoId, "the playing one stays where it is")
        assertEquals(1, count("y"), "and isn't queued again as the page's copy")
        assertEquals(8, q.items.size)
        assertTrue(q.shuffleOrig!!.any { it.videoId == "y" }, "the snapshot keeps the playlist whole")

        // Heard before its page came: it isn't queued a second time either.
        val heard = queue(listOf(sug("h"), song("now"), song("p0")), current = 1, orig = listOf(song("now"), song("p0")), dynamic = true)
        heard.appendPage(listOf(song("h"), song("p1")), playing = true, whole = false)
        assertEquals(1, heard.items.count { it.videoId == "h" })
        assertFalse(heard.items[0].suggested)

        // Next up, then plain shuffle before it plays: it's the playlist's song now, so it stays.
        val next = queue(listOf(song("now"), sug("s"), song("p0")), orig = listOf(song("now"), song("p0")), dynamic = true)
        next.appendPage(listOf(song("s")), playing = true, whole = false)
        assertTrue(next.setShuffle(ShuffleMode.On, whole = false))
        assertEquals(1, next.items.count { it.videoId == "s" })
    }
}
