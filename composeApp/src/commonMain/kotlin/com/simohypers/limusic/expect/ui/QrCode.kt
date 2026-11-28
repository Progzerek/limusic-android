package com.simohypers.limusic.expect.ui

import androidx.compose.runtime.Composable

/** A QR code's modules, row by row, without the quiet zone around them. */
class QrMatrix(
    val size: Int,
    private val modules: BooleanArray,
) {
    init {
        require(modules.size == size * size)
    }

    /** Dark or not; anything outside the code is light. */
    operator fun get(
        x: Int,
        y: Int,
    ): Boolean = x in 0 until size && y in 0 until size && modules[y * size + x]

    /** Inside one of the three 7×7 finder patterns ("eyes") or the light ring around it. */
    fun inFinder(
        x: Int,
        y: Int,
    ): Boolean = (x < 8 && y < 8) || (x >= size - 8 && y < 8) || (x < 8 && y >= size - 8)

    /**
     * The modules left blank in the middle for the logo: an odd span about a fifth of the code,
     * which error correction level H reads through. 0 from version 7 (45 modules) on, where an
     * alignment pattern sits in the middle.
     */
    val logoSpan: Int get() = if (size >= 45) 0 else (size / 5) or 1

    fun inLogo(
        x: Int,
        y: Int,
    ): Boolean {
        val span = logoSpan
        if (span == 0) return false
        val start = (size - span) / 2
        return x in start until start + span && y in start until start + span
    }
}

/** [text] as a QR code at error correction level H (room for the logo), or null if it can't be. */
expect fun encodeQr(text: String): QrMatrix?

/** While composed: the screen at full brightness and kept on, so a camera held up to it reads the code. */
@Composable
expect fun KeepScreenBright()
