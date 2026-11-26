package com.simohypers.limusic.expect.ui

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.simohypers.limusic.viewModel.ListenTogetherViewModel
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The room QR code as RoomQrCode draws it (corners rounded off lone modules, rounded eyes, a
 * blank middle under the logo, a 2.5-module quiet zone) still reads with ZXing's camera-style
 * detector, at several sizes and with the logo's colours as noise over the middle.
 */
class RoomQrDecodeTest {
    private val links =
        listOf(
            ListenTogetherViewModel.makeLink("", "QWERTYUP"),
            ListenTogetherViewModel.makeLink("", "12345678"),
            ListenTogetherViewModel.makeLink("wss://sync.example.org/ws", "ZXCVBNM1"),
            ListenTogetherViewModel.makeLink("ws://192.168.100.200:8099/ws", "ASDFGHJK"),
        )

    @Test
    fun aDefaultRoomIsAVersion4Code() {
        val m = assertNotNull(encodeQr(links[0]))
        assertEquals(33, m.size)
        assertEquals(7, m.logoSpan)
    }

    @Test
    fun finderAreasHoldTheEyes() {
        val m = assertNotNull(encodeQr(links[0]))
        // An eye's outer ring and core are dark, the ring between them light.
        for ((ex, ey) in listOf(0 to 0, m.size - 7 to 0, 0 to m.size - 7)) {
            assertTrue(m[ex, ey] && m[ex + 3, ey + 3] && !m[ex + 1, ey + 1])
            assertTrue(m.inFinder(ex + 6, ey + 6))
        }
    }

    @Test
    fun styledCodesDecode() {
        for (link in links) {
            val m = assertNotNull(encodeQr(link), link)
            for (px in listOf(4, 7, 12)) {
                for (noisyLogo in listOf(false, true)) {
                    assertEquals(link, decode(m, px, noisyLogo), "$link at $px px, noisy logo $noisyLogo")
                }
            }
        }
    }

    private fun decode(
        m: QrMatrix,
        px: Int,
        noisyLogo: Boolean,
    ): String? {
        val quiet = 2.5f
        val side = ((m.size + 2 * quiet) * px).toInt()
        val pixels = IntArray(side * side)
        val random = Random(42)
        for (y in 0 until side) {
            for (x in 0 until side) {
                val u = x.toFloat() / px - quiet + 0.5f / px
                val v = y.toFloat() / px - quiet + 0.5f / px
                val gray =
                    when {
                        u < 0 || v < 0 || u >= m.size || v >= m.size -> 255
                        m.inLogo(floor(u).toInt(), floor(v).toInt()) -> if (noisyLogo && random.nextBoolean()) 60 else 255
                        else -> eye(m, u, v) ?: if (module(m, u, v)) 20 else 255
                    }
                pixels[y * side + x] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
        return runCatching { QRCodeReader().decode(bitmap, mapOf(DecodeHintType.TRY_HARDER to true)).text }.getOrNull()
    }

    /** Inside an eye's box: its gray if on the ring or core, white between; null outside any eye. */
    private fun eye(
        m: QrMatrix,
        u: Float,
        v: Float,
    ): Int? {
        for ((ex, ey) in listOf(0 to 0, m.size - 7 to 0, 0 to m.size - 7)) {
            val a = u - ex
            val b = v - ey
            if (a < 0 || b < 0 || a >= 7 || b >= 7) continue
            val ring = inRoundRect(a, b, 0f, 7f, 2.4f) && !inRoundRect(a, b, 1f, 6f, 1.4f)
            val core = inRoundRect(a, b, 2f, 5f, 0.95f)
            return if (ring || core) 75 else 255
        }
        return if (m.inFinder(u.toInt(), v.toInt())) 255 else null
    }

    private fun module(
        m: QrMatrix,
        u: Float,
        v: Float,
    ): Boolean {
        val x = floor(u).toInt()
        val y = floor(v).toInt()
        fun dark(
            i: Int,
            j: Int,
        ) = m[i, j] && !m.inFinder(i, j) && !m.inLogo(i, j)
        if (!dark(x, y)) return false
        val fx = u - x
        val fy = v - y
        val left = fx < 0.5f
        val top = fy < 0.5f
        val rounded = !dark(if (left) x - 1 else x + 1, y) && !dark(x, if (top) y - 1 else y + 1)
        return !rounded || hypot(fx - 0.5f, fy - 0.5f) <= 0.5f
    }

    private fun inRoundRect(
        a: Float,
        b: Float,
        from: Float,
        to: Float,
        r: Float,
    ): Boolean {
        if (a < from || b < from || a > to || b > to) return false
        val cx = a.coerceIn(from + r, to - r)
        val cy = b.coerceIn(from + r, to - r)
        return hypot(a - cx, b - cy) <= r
    }
}
