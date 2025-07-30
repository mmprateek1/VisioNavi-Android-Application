package appinventor.ai_arm25412.vistech.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun FaceAnalysisScreen() {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("No face detected.") }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }
    var faceBoundingBoxes by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var frameWidth by remember { mutableStateOf(1) }
    var frameHeight by remember { mutableStateOf(1) }
    var currentFaceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showTrainDialog by remember { mutableStateOf(false) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var lastSpokenTime by remember { mutableStateOf(0L) }
    var lastSpokenMessage by remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var t: TextToSpeech? = null
        t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                t?.language = Locale.US
            }
        }
        t.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
        tts = t
        onDispose { tts?.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera preview layer
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrame = { bitmap: Bitmap ->
                frameWidth = bitmap.width
                frameHeight = bitmap.height
                val image = InputImage.fromBitmap(bitmap, 0)
                detector.process(image)
                    .addOnSuccessListener { faces: List<Face> ->
                        if (faces.isNotEmpty()) {
                            val count = faces.size
                            val message = if (count == 1) "person detected" else "$count people detected"
                            resultText = message.replaceFirstChar { it.uppercase() }
                            faceBoundingBoxes = faces.map { it.boundingBox }
                            // Crops the first detected face for training (for demo)
                            val rect = faces.first().boundingBox
                            try {
                                val faceBitmap = Bitmap.createBitmap(
                                    bitmap,
                                    rect.left.coerceAtLeast(0),
                                    rect.top.coerceAtLeast(0),
                                    rect.width().coerceAtMost(bitmap.width - rect.left),
                                    rect.height().coerceAtMost(bitmap.height - rect.top)
                                )
                                currentFaceBitmap = faceBitmap
                            } catch (e: Exception) {
                                currentFaceBitmap = null
                            }
                            val now = System.currentTimeMillis()
                            // Speak only if time passed or message changed and not already speaking
                            if ((now - lastSpokenTime > 1200 || message != lastSpokenMessage) && !isSpeaking) {
                                val params = Bundle()
                                val utteranceId = System.currentTimeMillis().toString()
                                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                                lastSpokenTime = now
                                lastSpokenMessage = message
                            }
                        } else {
                            resultText = "No face detected."
                            faceBoundingBoxes = emptyList()
                            currentFaceBitmap = null
                            lastSpokenMessage = ""
                        }
                    }
                    .addOnFailureListener {
                        resultText = "Face analysis failed."
                        faceBoundingBoxes = emptyList()
                        currentFaceBitmap = null
                        lastSpokenMessage = ""
                    }
            }
        )

        // Overlay rectangles using Canvas (draw over the preview)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (faceBoundingBoxes.isNotEmpty() && frameWidth > 0 && frameHeight > 0) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val scaleX = canvasWidth / frameWidth.toFloat()
                val scaleY = canvasHeight / frameHeight.toFloat()
                faceBoundingBoxes.forEach { rect ->
                    val left = rect.left * scaleX
                    val top = rect.top * scaleY
                    val right = rect.right * scaleX
                    val bottom = rect.bottom * scaleY
                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 6f)
                    )
                }
            }
        }

        // Info Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color(0x99000000))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Face Analysis",
                color = Color.Yellow,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = resultText,
                color = Color.White,
                fontSize = 20.sp
            )
        }

        // Train button (bottom center)
        Button(
            onClick = { showTrainDialog = true },
            enabled = currentFaceBitmap != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text("Train Face")
        }

        // Dialog to enter name and save the face
        if (showTrainDialog && currentFaceBitmap != null) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showTrainDialog = false },
                title = { Text("Enter Name") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Person Name") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        saveFaceForTraining(currentFaceBitmap!!, name, context)
                        showTrainDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = { showTrainDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// Save the cropped face bitmap for "training"
fun saveFaceForTraining(faceBitmap: Bitmap, name: String, context: Context) {
    val facesDir = File(context.filesDir, "faces")
    facesDir.mkdirs()
    val file = File(facesDir, "${name}_${System.currentTimeMillis()}.png")
    val out = FileOutputStream(file)
    faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    out.close()
}