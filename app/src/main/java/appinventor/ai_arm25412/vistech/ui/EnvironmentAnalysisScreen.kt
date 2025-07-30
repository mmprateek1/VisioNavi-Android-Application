package appinventor.ai_arm25412.vistech.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import appinventor.ai_arm25412.vistech.ObjectDetectorHelper

@Composable
fun EnvironmentAnalysisScreen() {
    val context = LocalContext.current
    val objectDetectorHelper = remember { ObjectDetectorHelper(context) }
    var summaryText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        val bitmap = imageProxyToBitmap(imageProxy)
                        if (bitmap != null) {
                            val results = objectDetectorHelper.detectAndSpeak(bitmap)
                            val labels = results.mapNotNull { detection ->
                                detection.categories.firstOrNull()?.label
                            }
                            val grouped = labels.groupingBy { it }.eachCount()
                            val scenePhrase = generateSceneDescription(grouped)
                            summaryText = scenePhrase
                            objectDetectorHelper.speakPhrase(scenePhrase)
                        }
                        imageProxy.close()
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
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 32.dp, start = 12.dp, end = 12.dp)
                .background(Color(0x88000000)) // semi-transparent black for visibility
        ) {
            Text(
                text = summaryText,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 6f
                    )
                )
            )
        }
    }
}

/**
 * Generates a natural language phrase from detected objects for accessibility.
 * Example: "There is a laptop and a cup in front of you."
 */
private fun generateSceneDescription(objects: Map<String, Int>): String {
    if (objects.isEmpty()) return "No objects detected."

    val objectList = objects.entries
        .sortedByDescending { it.value }
        .map { (label, count) ->
            when (count) {
                1 -> "a $label"
                2 -> "two ${label}s"
                3 -> "three ${label}s"
                else -> "$count ${label}s"
            }
        }
    return when (objectList.size) {
        1 -> "There is ${objectList[0]} in front of you."
        2 -> "There is ${objectList[0]} and ${objectList[1]} in front of you."
        else -> "There is ${objectList.dropLast(1).joinToString(", ")}, and ${objectList.last()} in front of you."
    }
}