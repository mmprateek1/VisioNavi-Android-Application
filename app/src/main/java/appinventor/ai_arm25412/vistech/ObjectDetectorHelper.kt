package appinventor.ai_arm25412.vistech

import android.content.Context
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.Locale

class ObjectDetectorHelper(context: Context) : TextToSpeech.OnInitListener {
    private val detector: ObjectDetector
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private val objectLastSpokenTime = mutableMapOf<String, Long>()
    private val objectCooldownMillis = 3000L // 3 seconds cooldown per object

    private var ttsReady = false
    private var lastSpokenPhrase = ""
    private var isTtsSpeaking = false
    private var pendingPhrase: String? = null

    init {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(0.3f)
            .setMaxResults(5)
            .build()
        detector = ObjectDetector.createFromFileAndOptions(
            context,
            "efficientdet_lite3.tflite",
            options
        )

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isTtsSpeaking = true
            }
            override fun onDone(utteranceId: String?) {
                isTtsSpeaking = false
                pendingPhrase?.let {
                    speakPhrase(it)
                    pendingPhrase = null
                }
            }
            override fun onError(utteranceId: String?) {
                isTtsSpeaking = false
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.85f) // Slow and clear for accessibility
            ttsReady = true
        }
    }

    fun detectAndSpeak(bitmap: Bitmap): List<Detection> {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val detections = detector.detect(tensorImage)
        val detectedLabels = detections.mapNotNull { detection ->
            detection.categories.firstOrNull()?.label
        }
        speakNewObjects(detectedLabels)
        return detections
    }

    private fun speakNewObjects(detectedObjects: List<String>) {
        val now = System.currentTimeMillis()
        val toSpeak = detectedObjects.distinct().filter { obj ->
            val lastTime = objectLastSpokenTime[obj] ?: 0L
            (now - lastTime) > objectCooldownMillis
        }
        if (toSpeak.isNotEmpty()) {
            val speechText = toSpeak.joinToString(", ")
            speakPhrase(speechText)
            toSpeak.forEach { obj -> objectLastSpokenTime[obj] = now }
        }
    }

    fun speakPhrase(text: String) {
        if (!ttsReady) return
        if (text == lastSpokenPhrase) return
        if (isTtsSpeaking) {
            pendingPhrase = text
            return
        }
        lastSpokenPhrase = text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SceneDesc")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}