package com.simohypers.limusic.data.listentogether

import com.simohypers.limusic.listentogether.ListenTogetherClient

/**
 * DataStore keys for Listen Together.
 *
 * They live here rather than next to the screen because both sides need them: the screen writes
 * the server URL, and the DI module that builds the client reads it before any screen exists.
 */
object ListenTogetherPrefs {
    /** Empty means [DEFAULT_SERVER_URL]. */
    const val SERVER_URL = "lt_server_url"

    /** Hosting: join requests are approved without asking. TRUE or FALSE, off when unset. */
    const val AUTO_ADMIT = "lt_auto_admit"
    const val DEFAULT_SERVER_URL = ListenTogetherClient.DEFAULT_SERVER_URL
}
