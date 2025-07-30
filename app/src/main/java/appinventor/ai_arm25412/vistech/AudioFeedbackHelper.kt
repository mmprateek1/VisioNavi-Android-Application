package appinventor.ai_arm25412.vistech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

class AudioFeedbackHelper(context: Context) {
    private val tts: TextToSpeech
    private val speechQueue: Queue<String> = LinkedList()
    private var isSpeaking = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setSpeechRate(0.7f) // <--- Slow down the speech (try 0.8f or 0.7f)
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // do nothing
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                speakNext()
            }
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                speakNext()
            }
        })
    }

    fun speak(text: String) {
        speechQueue.add(text)
        if (!isSpeaking) {
            speakNext()
        }
    }

    private fun speakNext() {
        if (speechQueue.isNotEmpty() && !isSpeaking) {
            val nextText = speechQueue.poll()
            isSpeaking = true
            tts.speak(nextText, TextToSpeech.QUEUE_ADD, null, System.currentTimeMillis().toString())
        }
    }
    fun shutdown() {
        tts.shutdown()
    }
}