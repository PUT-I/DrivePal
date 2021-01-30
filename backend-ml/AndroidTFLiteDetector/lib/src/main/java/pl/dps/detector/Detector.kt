package pl.dps.detector

import android.graphics.Bitmap
import android.graphics.RectF
import org.json.JSONObject
import pl.dps.detector.enums.DetectionModel

/**
 * Generic interface for interacting with different recognition engines.
 */
interface Detector {
    fun getDetectionModel(): DetectionModel

    fun recognizeImage(bitmap: Bitmap, returnImage: Boolean = false): DetectionResults

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    class Detection(
            /**
             * A unique identifier for what has been recognized. Specific to the class, not the instance of
             * the object.
             */
            private val id: String,
            /**
             * Display name for the recognition.
             */
            val className: String,
            /**
             * A sortable score for how good the recognition is relative to others. Higher should be better.
             */
            val confidence: Float,
            /**
             * Optional location within the source image for the location of the recognized object.
             */
            var boundingBox: RectF,
            val detectedClass: Int) {

        override fun toString(): String {
            var resultString = "[$id] $className "
            resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            resultString += "$boundingBox "
            return resultString.trim { it <= ' ' }
        }

        fun toJSON(): JSONObject {
            val result = JSONObject()

            result.put("className", className)
            result.put("confidence", confidence)
            result.put("locationLeft", boundingBox.left)
            result.put("locationTop", boundingBox.top)
            result.put("locationRight", boundingBox.right)
            result.put("locationBottom", boundingBox.bottom)

            return result
        }
    }
}