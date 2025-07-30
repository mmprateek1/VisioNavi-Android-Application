package appinventor.ai_arm25412.vistech.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import appinventor.ai_arm25412.vistech.ObjectDetectorHelper
import org.tensorflow.lite.task.vision.detector.Detection

const val REQUEST_CODE = 1001

private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PathNavigationScreen(
    activity: Activity,
    speechRecognitionHelper: SpeechRecognitionHelper,
    targetObject: String?,
    onTargetObjectRecognized: (String?) -> Unit // Nullable to allow cancel
) {
    val context = LocalContext.current
    val objectDetectorHelper = remember { ObjectDetectorHelper(context) }
    val navigationStarted = targetObject != null
    val showCamera = targetObject != null

    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    var targetMissingCount by remember { mutableStateOf(0) }
    var lastWarnedMissing by remember { mutableStateOf(false) }
    val maxMissingFrames = 15

    var lastGuidance by remember { mutableStateOf("") }
    var lastDistance by remember { mutableStateOf<Float?>(null) }
    var lastDetectedBoxCenter by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // For repeating guidance every second even if not changed
    var guidanceRepeatCounter by remember { mutableStateOf(0) }
    val guidanceRepeatInterval = 30 // approx every second if 30fps

    // For spoken message throttling
    var lastSpokenGuidance by remember { mutableStateOf("") }
    var lastGuidanceFrame by remember { mutableStateOf(0) }

    fun speakGuidance(guidance: String, frame: Int) {
        // Repeat at intervals OR if changed
        if (guidance != lastSpokenGuidance || (frame - lastGuidanceFrame) > guidanceRepeatInterval) {
            objectDetectorHelper.speakPhrase(guidance)
            lastSpokenGuidance = guidance
            lastGuidanceFrame = frame
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!navigationStarted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Button(
                    onClick = {
                        // Start voice recognition for a new target object
                        speechRecognitionHelper.startListening(REQUEST_CODE)
                    },
                    modifier = Modifier
                        .height(80.dp)
                        .width(250.dp)
                ) {
                    Text(
                        text = "Start Navigation",
                        fontSize = 28.sp
                    )
                }
            }
        } else if (showCamera && targetObject != null) {
            Box(Modifier.fillMaxSize()) {
                // Cancel button always visible at top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                ) {
                    Button(onClick = {
                        // Cancel navigation: clear target and return to voice command input
                        onTargetObjectRecognized(null)
                    }) {
                        Text("Cancel", color = Color.White)
                    }
                }

                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrame = { bitmap: Bitmap ->
                        val results = objectDetectorHelper.detectAndSpeak(bitmap)
                        detections = results
                        imageWidth = bitmap.width
                        imageHeight = bitmap.height

                        val detectedLabels = results.mapNotNull { it.categories.firstOrNull()?.label?.lowercase() }
                        val targetLabel = targetObject.lowercase()
                        val targetDetection = results.find { it.categories.firstOrNull()?.label?.lowercase() == targetLabel }

                        if (targetDetection != null) {
                            targetMissingCount = 0
                            lastWarnedMissing = false

                            val box = targetDetection.boundingBox
                            val boxArea = (box.right - box.left) * (box.bottom - box.top)
                            lastDistance = 1f / (boxArea + 1f)

                            val cx = (box.left + box.right) / 2f
                            val imgCx = imageWidth / 2f
                            val dx = cx - imgCx
                            val normDx = dx / imageWidth
                            val direction = when {
                                normDx > 0.15f -> "right"
                                normDx < -0.15f -> "left"
                                else -> "center"
                            }
                            val distPhrase = when {
                                boxArea > 0.35f * imageWidth * imageHeight -> "You have reached the $targetObject."
                                boxArea > 0.15f * imageWidth * imageHeight -> "The $targetObject is very close."
                                boxArea > 0.07f * imageWidth * imageHeight -> "The $targetObject is ahead, a few steps away."
                                else -> "The $targetObject is ahead, keep moving."
                            }
                            val guidancePhrase = when (direction) {
                                "left" -> "Move left towards the $targetObject. $distPhrase"
                                "right" -> "Move right towards the $targetObject. $distPhrase"
                                else -> "Go straight. $distPhrase"
                            }
                            lastGuidance = guidancePhrase

                            speakGuidance(guidancePhrase, guidanceRepeatCounter)
                            guidanceRepeatCounter++

                            // Vibrate if reached
                            if (boxArea > 0.35f * imageWidth * imageHeight) {
                                getVibrator(context)?.vibrate(
                                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            }
                        } else {
                            targetMissingCount++
                            if (targetMissingCount >= maxMissingFrames && !lastWarnedMissing) {
                                lastWarnedMissing = true
                                objectDetectorHelper.speakPhrase("Cannot see the $targetObject, please move your device.")
                                getVibrator(context)?.vibrate(
                                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                                val grouped = detectedLabels.groupingBy { it }.eachCount()
                                val scenePhrase = generateSceneDescription(grouped)
                                if (scenePhrase.isNotBlank()) {
                                    objectDetectorHelper.speakPhrase(scenePhrase)
                                }
                            }
                        }
                    }
                )

                // Overlay bounding boxes, labels, and info
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val wScale = size.width / imageWidth
                    val hScale = size.height / imageHeight
                    detections.forEach { detection ->
                        val category = detection.categories.firstOrNull()
                        val label = category?.label ?: "Unknown"
                        val score = category?.score ?: 0f
                        val rect = detection.boundingBox ?: return@forEach
                        val rectF = Rect(
                            left = rect.left * wScale,
                            top = rect.top * hScale,
                            right = rect.right * wScale,
                            bottom = rect.bottom * hScale
                        )
                        val boxColor = if (label.equals(targetObject, ignoreCase = true)) Color(0xFF00FF00) else Color.Red
                        drawRect(
                            color = boxColor,
                            topLeft = rectF.topLeft,
                            size = rectF.size,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                        )
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = if (label.equals(targetObject, ignoreCase = true)) android.graphics.Color.GREEN else android.graphics.Color.RED
                                textSize = 26.dp.toPx()
                                setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
                            }
                            val text = "$label ${(score * 100).toInt()}%"
                            canvas.nativeCanvas.drawText(
                                text,
                                rect.left * wScale,
                                (rect.top * hScale - 10).coerceAtLeast(40f),
                                paint
                            )
                        }
                    }
                }

                // Status overlay - centered at the top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 20.dp)
                        .background(Color(0x66000000))
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Target: ${targetObject.capitalize()}",
                            fontSize = 32.sp,
                            color = Color.Yellow,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (lastGuidance.isNotEmpty()) {
                            Text(
                                text = lastGuidance,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                        if (lastWarnedMissing) {
                            Text(
                                text = "Looking for $targetObject...",
                                fontSize = 24.sp,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun generateSceneDescription(objects: Map<String, Int>): String {
    if (objects.isEmpty()) return ""
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
        1 -> "There's ${objectList[0]} in front of you."
        2 -> "There's ${objectList[0]} and ${objectList[1]} in front of you."
        else -> "There's ${objectList.dropLast(1).joinToString(", ")}, and ${objectList.last()} in front of you."
    }
}