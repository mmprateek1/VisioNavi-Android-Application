package appinventor.ai_arm25412.vistech.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent

class SpeechRecognitionHelper(private val activity: Activity) {
    fun startListening(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Navigate to {object}")
        activity.startActivityForResult(intent, requestCode)
    }
}