package appinventor.ai_arm25412.vistech.ui

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

data class FaceAnalysisResult(
    val description: String
)

class FaceAnalyzer(
    context: Context? = null // not used, but can pass for later TTS
) {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    fun analyze(bitmap: Bitmap): FaceAnalysisResult? {
        val image = InputImage.fromBitmap(bitmap, 0)
        var result: FaceAnalysisResult? = null
        detector.process(image)
            .addOnSuccessListener { faces: List<Face> ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val smilingProb = face.smilingProbability ?: -1f
                    val leftEyeProb = face.leftEyeOpenProbability ?: -1f
                    val rightEyeProb = face.rightEyeOpenProbability ?: -1f

                    val desc = buildString {
                        append("Face detected.\n")
                        if (smilingProb >= 0) append("Smiling: ${"%.1f".format(smilingProb * 100)}%\n")
                        if (leftEyeProb >= 0) append("Left Eye Open: ${"%.1f".format(leftEyeProb * 100)}%\n")
                        if (rightEyeProb >= 0) append("Right Eye Open: ${"%.1f".format(rightEyeProb * 100)}%\n")
                    }
                    result = FaceAnalysisResult(desc)
                }
            }
            .addOnFailureListener {
                result = null
            }
        return result
    }
}