package appinventor.ai_arm25412.vistech

import android.graphics.RectF
import org.tensorflow.lite.task.vision.detector.Detection

class TrackedObject(
    val id: Int,
    var detection: Detection,
    var lastFrame: Int
)

class CentroidTracker(
    private val maxDistance: Float = 100f,
    private val maxLostFrames: Int = 10
) {
    private val objects = mutableMapOf<Int, TrackedObject>()
    private var nextId = 1

    fun update(detections: List<Detection>, currentFrame: Int): List<TrackedObject> {
        val updatedObjects = mutableListOf<TrackedObject>()
        val unmatchedDetections = detections.toMutableList()

        for ((_, obj) in objects) {
            var bestMatch: Detection? = null
            var bestDist = Float.MAX_VALUE
            for (det in unmatchedDetections) {
                val dist = centroidDistance(obj.detection.boundingBox, det.boundingBox)
                if (dist < bestDist && dist < maxDistance) {
                    bestDist = dist
                    bestMatch = det
                }
            }
            if (bestMatch != null) {
                obj.detection = bestMatch
                obj.lastFrame = currentFrame
                updatedObjects.add(obj)
                unmatchedDetections.remove(bestMatch)
            }
        }

        for (det in unmatchedDetections) {
            val obj = TrackedObject(nextId++, det, currentFrame)
            objects[obj.id] = obj
            updatedObjects.add(obj)
        }

        // Remove lost objects (API 23 compatible)
        val iterator = objects.values.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            if (currentFrame - obj.lastFrame > maxLostFrames) {
                iterator.remove()
            }
        }

        return updatedObjects
    }

    private fun centroidDistance(b1: RectF, b2: RectF): Float {
        val c1x = (b1.left + b1.right) / 2
        val c1y = (b1.top + b1.bottom) / 2
        val c2x = (b2.left + b2.right) / 2
        val c2y = (b2.top + b2.bottom) / 2
        return Math.hypot((c1x - c2x).toDouble(), (c1y - c2y).toDouble()).toFloat()
    }
}