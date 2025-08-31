# VisioNavi Android App

VisioNavi is an Android application designed to assist visually impaired users with real-time navigation and environmental awareness. Part of the Real-Time Monocular Vision Object Detection and Navigational Assistance System (RT-MVODNAS), it leverages computer vision and AI to provide object detection, depth estimation, face analysis, and navigational guidance using standard smartphone hardware.

## Features
- **Real-Time Object Detection:** Identifies objects using YOLOv5 via TensorFlow Lite.
- **Depth Estimation:** Estimates spatial distances with MiDaS for obstacle avoidance.
- **Path Planning:** Computes obstacle-free paths using the A* algorithm.
- **Face Analysis:** Detects and analyzes faces (emotion, age, gender) with Google ML Kit.
- **Scene Description:** Generates natural language descriptions via BLIP, delivered as audio.
- **Accessible Interface:** Built with Jetpack Compose, supports voice commands and audio/haptic feedback.

## Technologies Used
- **Language:** Kotlin (primary), Java (optional)
- **Frontend:** Jetpack Compose
- **Backend:** TensorFlow Lite, Google ML Kit, CameraX
- **APIs:** Speech Recognition API, TextToSpeech API, Vibration API
- **Tools:** Android Studio, Gradle

## Prerequisites
- **Hardware:**
  - Android device with Android 8.0+ (Oreo)
  - Quad-core ARM Cortex A53 processor (Octa-core recommended)
  - 2 GB RAM (4 GB recommended)
  - 8 MP rear camera with autofocus
  - 500 MB free storage
  - Speaker, microphone, vibration motor (recommended)
- **Software:**
  - Android Studio (latest version)
  - Kotlin 1.8+
  - Gradle 7.0+
- **Permissions:** Camera, Microphone, Storage, Vibration

## Installation and Setup
1. **Clone the Repository:**
   ```bash
   git clone https://github.com/hruthikchauhan07/VisioNavi-Android-App.git
   cd VisioNavi-Android-App
   ```
2. **Open in Android Studio:**
   - Launch Android Studio and select "Open an existing project."
   - Navigate to the cloned repository folder and open it.
3. **Configure Dependencies:**
   - Ensure `build.gradle` includes:
     ```gradle
     implementation 'androidx.compose:compose-bom:2023.10.01'
     implementation 'androidx.camera:camerax:1.3.0'
     implementation 'org.tensorflow:tensorflow-lite:2.10.0'
     implementation 'com.google.mlkit:face-detection:16.1.5'
     ```
   - Sync the project with Gradle.
4. **Grant Permissions:**
   - Enable Camera, Microphone, Storage, and Vibration permissions in the Android manifest.
5. **Build and Run:**
   - Connect an Android device or use an emulator.
   - Click "Run" in Android Studio to build and deploy the app.

## Usage
1. **Launch the App:** Open VisioNavi on your Android device.
2. **Main Menu:** Access features via the Jetpack Compose UI:
   - Object Detection: Identifies objects in real-time.
   - Path Navigation: Guides users with audio/haptic feedback.
   - Face Analysis: Provides face recognition and attributes.
   - Environment Analysis: Delivers audio scene descriptions.
3. **Voice Commands:** Use the microphone (floating action button) to interact hands-free.
4. **Feedback:** Receive audio (TextToSpeech) and haptic cues for navigation and alerts.

## Project Structure
- `app/src/main/java/appinventor.ai_arns2412.vistech`: Main package with app logic.
- `MainMenuScreen.kt`: Jetpack Compose UI for the main menu.
- `NavHelper.kt`: Navigation host for routing between screens (Object Detection, Path Navigation, etc.).
- `res/`: Resources for layouts, drawables, and assets.
- `build.gradle`: Dependency configurations for TensorFlow Lite, CameraX, and ML Kit.

## Contributing
Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Commit changes (`git commit -m "Add feature"`).
4. Push to the branch (`git push origin feature-branch`).
5. Open a pull request.

Please ensure code follows Kotlin conventions and includes comments for clarity.


## Contact
- **Developer: M M Prateek**
  - Phone Number: [+91 7975151758]
  - Email: [mmprateek2004@gmail.com]

- **co-Developer: Hruthik M**
  - Phone Number: [+91 6366685912]
  - Email: [hruthikmnaik07@gmail.com]
