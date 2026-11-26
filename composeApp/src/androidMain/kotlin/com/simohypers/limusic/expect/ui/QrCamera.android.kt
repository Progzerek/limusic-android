package com.simohypers.limusic.expect.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Size
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.simohypers.limusic.logger.Logger
import java.util.concurrent.Executors

/*
 * CameraX + ZXing rather than ML Kit or Google's code scanner: both of those are proprietary.
 * QR is found by its eyes, so frames are decoded as the sensor delivers them, unrotated, and the
 * eyes are mapped into the preview afterwards for the scanner's frame to land on.
 */
@Composable
actual fun QrCameraPreview(
    onSighting: (QrSighting) -> Unit,
    onTrouble: (CameraTrouble) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSighting by rememberUpdatedState(onSighting)
    val currentOnTrouble by rememberUpdatedState(onTrouble)
    // COMPATIBLE is a TextureView: a SurfaceView ignores Compose clipping, and the scanner opens
    // as a growing circle.
    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(lifecycleOwner) {
        val analyzerThread = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        // The provider can arrive after the scanner has closed; binding then would hold the
        // camera until the activity dies.
        var disposed = false
        val reader = QRCodeReader()
        // Frames come unrotated; the transform turns and crops them the way the preview shows them.
        val transforms =
            ImageProxyTransformFactory().apply {
                isUsingRotationDegrees = true
                isUsingCropRect = true
            }
        providerFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = providerFuture.get().also { provider = it }
                // Tablets and Chromebooks may only have a front camera.
                val selector =
                    when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> {
                            currentOnTrouble(CameraTrouble.None)
                            return@addListener
                        }
                    }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // 720p reads a code from across a table and still decodes in a few frames' time.
                        .setResolutionSelector(
                            ResolutionSelector
                                .Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                                ).build(),
                        ).build()
                analysis.setAnalyzer(analyzerThread) { image ->
                    image.use {
                        val result = decodeQr(it, reader) ?: return@use
                        val source = runCatching { transforms.getOutputTransform(it) }.getOrNull()
                        mainExecutor.execute {
                            if (!disposed) currentOnSighting(QrSighting(result.text, eyesInPreview(result, source, previewView)))
                        }
                    }
                }
                cameraProvider.unbindAll()
                // One viewport for both: each frame's crop is then what the preview shows, so only a
                // code on screen is read and its eyes map onto the screen exactly.
                val camera =
                    previewView.viewPort?.let { viewPort ->
                        val group =
                            UseCaseGroup
                                .Builder()
                                .setViewPort(viewPort)
                                .addUseCase(preview)
                                .addUseCase(analysis)
                                .build()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, group)
                    } ?: cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                // Another app taking the camera is recoverable and CameraX waits for it; a policy
                // block or a dead camera is not.
                camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                    val error = state.error ?: return@observe
                    Logger.w(TAG, "camera state error ${error.code}")
                    if (error.type == CameraState.ErrorType.CRITICAL && !disposed) currentOnTrouble(CameraTrouble.Failed)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "camera: ${e.message}")
                currentOnTrouble(CameraTrouble.Failed)
            }
        }, mainExecutor)
        onDispose {
            disposed = true
            provider?.unbindAll()
            analyzerThread.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private const val TAG = "QrCamera"

private val DECODE_HINTS = mapOf(DecodeHintType.TRY_HARDER to true)

/** Reads the luminance (Y) plane only, which is all ZXing needs. */
private fun decodeQr(
    image: ImageProxy,
    reader: QRCodeReader,
): Result? {
    val plane = image.planes[0]
    val buffer = plane.buffer
    // rowStride, not width, is the real distance between rows: many sensors pad each row, and the
    // last row may come without its padding.
    val bytes = ByteArray(plane.rowStride * image.height)
    buffer.get(bytes, 0, minOf(buffer.remaining(), bytes.size))
    val crop = Rect(image.cropRect).takeIf { it.intersect(0, 0, image.width, image.height) && !it.isEmpty } ?: Rect(0, 0, image.width, image.height)
    val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, image.height, crop.left, crop.top, crop.width(), crop.height(), false)
    return try {
        // Points come back relative to the crop; the transform wants the whole frame's.
        val result = reader.decode(BinaryBitmap(HybridBinarizer(source)), DECODE_HINTS)
        val points = result.resultPoints?.map { ResultPoint(it.x + crop.left, it.y + crop.top) }?.toTypedArray()
        Result(result.text, result.rawBytes, points, result.barcodeFormat)
    } catch (e: ReaderException) {
        null
    } catch (e: RuntimeException) {
        null
    } finally {
        reader.reset()
    }
}

/** The code's three eyes in the preview's pixels; null until the preview can say where it draws. */
private fun eyesInPreview(
    result: Result,
    source: OutputTransform?,
    previewView: PreviewView,
): List<Offset>? {
    val points = result.resultPoints ?: return null
    if (points.size < 3 || source == null) return null
    val target = previewView.outputTransform ?: return null
    val xy = FloatArray(6) { i -> if (i % 2 == 0) points[i / 2].x else points[i / 2].y }
    CoordinateTransform(source, target).mapPoints(xy)
    return List(3) { Offset(xy[it * 2], xy[it * 2 + 1]) }
}

@Composable
actual fun rememberCameraPermission(onResult: (granted: Boolean, blocked: Boolean) -> Unit): CameraPermission {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnResult by rememberUpdatedState(onResult)
    // Android only says "won't ask again" indirectly: its rationale flag is false before the first
    // ask, true after one refusal, false again once it stops asking. A dialog closed with Back
    // leaves it false too, so a refusal counts as final only when the flag fell from true, or when
    // the answer came back too fast for anyone to have seen a dialog.
    val asked = remember { longArrayOf(0L, 0L) } // when, and whether the rationale was showing
    fun rationale() = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val unseen = System.currentTimeMillis() - asked[0] < 350L
            val blocked = !granted && !rationale() && (asked[1] == 1L || unseen)
            currentOnResult(granted, blocked)
        }
    return remember(context, launcher) {
        object : CameraPermission {
            override val granted get() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

            override val hasCamera get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

            override fun request() {
                asked[0] = System.currentTimeMillis()
                asked[1] = if (rationale()) 1L else 0L
                launcher.launch(Manifest.permission.CAMERA)
            }

            override fun openSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

@Composable
actual fun CameraDialogWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            window.setDimAmount(0f)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setWindowAnimations(0)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes =
                    window.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        onDispose { }
    }
}
