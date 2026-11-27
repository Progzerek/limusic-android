package com.simohypers.limusic.domain.data.player

import com.simohypers.limusic.domain.mediaservice.handler.RepeatState

sealed class GenericCommandButton {
    data class Like(
        val isLiked: Boolean,
    ) : GenericCommandButton()

    data class Shuffle(
        val isShuffled: Boolean,
        /** Dynamic shuffle, with [isShuffled]. */
        val isDynamic: Boolean = false,
    ) : GenericCommandButton()

    data class Repeat(
        val repeatState: RepeatState,
    ) : GenericCommandButton()

    data object Radio : GenericCommandButton()
}