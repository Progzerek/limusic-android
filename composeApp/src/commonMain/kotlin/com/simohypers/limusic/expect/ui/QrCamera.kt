package com.simohypers.limusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * A QR code the camera read: its text, and where its three eyes sit in the preview, in the
 * preview's pixels (bottom left, top left, top right), when the camera could say.
 */
class QrSighting(
    val text: String,
    val eyes: List<Offset>?,
)

enum class CameraTrouble {
    /** The device has no camera at all. */
    None,

    /** It has one, but it would not open (another app holds it, or it failed). */
    Failed,
}

/**
 * The back camera filling [modifier], reporting every QR code it reads. Stops with the screen's
 * lifecycle; the camera permission must already be granted.
 */
@Composable
expect fun QrCameraPreview(
    onSighting: (QrSighting) -> Unit,
    onTrouble: (CameraTrouble) -> Unit,
    modifier: Modifier = Modifier,
)

interface CameraPermission {
    /** Read fresh each time: the user can grant it in the system settings meanwhile. */
    val granted: Boolean

    /** The device has any camera. */
    val hasCamera: Boolean

    /** Android's dialog; the answer goes to the callback the permission was built with. */
    fun request()

    /** The app's page in the system settings, for when Android stops asking. */
    fun openSettings()
}

/** [onResult]'s `blocked` is true when Android will not show its dialog again. */
@Composable
expect fun rememberCameraPermission(onResult: (granted: Boolean, blocked: Boolean) -> Unit): CameraPermission

/**
 * Sets up the full-screen dialog window it is called in for a camera: no dim or window animation
 * of its own (the content animates), light system bar icons, the screen kept on.
 */
@Composable
expect fun CameraDialogWindow()
