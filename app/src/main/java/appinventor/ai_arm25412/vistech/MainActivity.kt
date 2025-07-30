package appinventor.ai_arm25412.vistech

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import appinventor.ai_arm25412.vistech.ui.EnvironmentAnalysisScreen
import appinventor.ai_arm25412.vistech.ui.FaceAnalysisScreen
import appinventor.ai_arm25412.vistech.ui.MainMenuScreen
import appinventor.ai_arm25412.vistech.ui.ObjectDetectionScreen
import appinventor.ai_arm25412.vistech.ui.PathNavigationScreen
import appinventor.ai_arm25412.vistech.ui.REQUEST_CODE
import appinventor.ai_arm25412.vistech.ui.SpeechRecognitionHelper

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognitionHelper: SpeechRecognitionHelper

    // Hoisted state for navigation target recognized from speech
    private var composeSetTargetObject: ((String?) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permission at runtime if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        // Initialize speech recognition helper
        speechRecognitionHelper = SpeechRecognitionHelper(this)

        setContent {
            var targetObject by remember { mutableStateOf<String?>(null) }
            // Provide a setter function for speech callback
            SideEffect {
                composeSetTargetObject = { obj -> targetObject = obj }
            }

            MaterialTheme {
                AppNavigator(
                    activity = this,
                    speechRecognitionHelper = speechRecognitionHelper,
                    targetObject = targetObject,
                    onNavigationTarget = { obj -> targetObject = obj } // obj is String? (nullable)
                )
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API...")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted: you can proceed
            } else {
                // Permission denied: show a message or close the feature
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            val regex = Regex("navigate to (\\w+)", RegexOption.IGNORE_CASE)
            val match = regex.find(spokenText)
            val targetObject = match?.groupValues?.get(1)?.lowercase()
            if (targetObject != null) {
                // Update Compose state when speech returns
                composeSetTargetObject?.invoke(targetObject)
                Toast.makeText(this, "Navigating to: $targetObject", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Could not understand the navigation command.", Toast.LENGTH_SHORT).show()
                composeSetTargetObject?.invoke(null)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalGetImage::class)
@Composable
fun AppNavigator(
    activity: MainActivity,
    speechRecognitionHelper: SpeechRecognitionHelper,
    targetObject: String?,
    onNavigationTarget: (String?) -> Unit // changed to nullable
) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(
                onObjectDetection = { navController.navigate("object_detection") },
                onPathNavigation = { navController.navigate("path_navigation") },
                onEnvironmentAnalysis = { navController.navigate("environment_analysis") },
                onFaceAnalysis = { navController.navigate("face_analysis") },
                onMicClick = { /* TODO: Implement voice assistant */ }
            )
        }
        composable("object_detection") { ObjectDetectionScreen() }
        composable("path_navigation") {
            PathNavigationScreen(
                activity = activity,
                speechRecognitionHelper = speechRecognitionHelper,
                targetObject = targetObject,
                onTargetObjectRecognized = { obj ->
                    onNavigationTarget(obj) // pass obj nullable
                }
            )
        }
        composable("environment_analysis") { EnvironmentAnalysisScreen() }
        composable("face_analysis") { FaceAnalysisScreen() }
    }
}