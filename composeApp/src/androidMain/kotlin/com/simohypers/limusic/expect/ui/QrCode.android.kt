package com.simohypers.limusic.expect.ui

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

// No character set hint: the links are ASCII, and naming UTF-8 would add an ECI block for nothing.
actual fun encodeQr(text: String): QrMatrix? =
    runCatching {
        val matrix = Encoder.encode(text, ErrorCorrectionLevel.H).matrix
        val size = matrix.width
        QrMatrix(size, BooleanArray(size * size) { matrix.get(it % size, it / size).toInt() == 1 })
    }.getOrNull()

@Composable
actual fun KeepScreenBright() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val before = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = before }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
