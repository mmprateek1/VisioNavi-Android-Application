package appinventor.ai_arm25412.vistech.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import appinventor.ai_arm25412.vistech.ObjectDetectorHelper
import org.tensorflow.lite.task.vision.detector.Detection

@Composable
fun ObjectDetectionScreen() {
    val context = LocalContext.current
    val objectDetectorHelper = remember { ObjectDetectorHelper(context) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var imageWidth by remember { mutableIntStateOf(1) }
    var imageHeight by remember { mutableIntStateOf(1) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                if (imageProxy.width > 0 && imageProxy.height > 0) {
                                    imageWidth = imageProxy.width
                                    imageHeight = imageProxy.height
                                }
                                val bitmap = imageProxyToBitmap(imageProxy)
                                if (bitmap != null) {
                                    val results = objectDetectorHelper.detectAndSpeak(bitmap)
                                    detections = results
                                }
                                imageProxy.close()
                            }
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for bounding boxes and labels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val wScale = size.width / imageWidth
            val hScale = size.height / imageHeight
            detections.forEach { detection ->
                val category = detection.categories.firstOrNull()
                val label = category?.label ?: "Unknown"
                val score = category?.score ?: 0f
                val rect = detection.boundingBox ?: return@forEach
                // Scale bounding box to the view size
                val rectF = Rect(
                    left = rect.left * wScale,
                    top = rect.top * hScale,
                    right = rect.right * wScale,
                    bottom = rect.bottom * hScale
                )
                // Draw rectangle
                drawRect(
                    color = Color.Red,
                    topLeft = rectF.topLeft,
                    size = rectF.size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
                // Draw label and score
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        textSize = 26.dp.toPx()
                        setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
                    }
                    val text = "$label ${(score * 100).toInt()}%"
                    // Draw text above the bounding box
                    canvas.nativeCanvas.drawText(
                        text,
                        rect.left * wScale,
                        (rect.top * hScale - 10).coerceAtLeast(40f),
                        paint
                    )
                }
            }
        }
    }
}

