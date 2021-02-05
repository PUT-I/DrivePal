package pl.dps.detector.visualization

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import pl.dps.detector.Detector.Detection
import pl.dps.detector.utils.ImageUtils.getTransformationMatrix
import java.util.*
import kotlin.math.min

/**
 * A tracker that handles non-max suppression and matches existing objects to new detections.
 */
class MultiBoxTracker(context: Context, private val showConfidence: Boolean = true) {

    companion object {
        private const val TAG = "MultiBoxTracker"
        private const val TEXT_SIZE_DIP = 18f
        private const val MIN_SIZE = 16.0f
        private val COLORS = intArrayOf(
                Color.BLUE,
                Color.RED,
                Color.GREEN,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.WHITE,
                Color.parseColor("#55FF55"),
                Color.parseColor("#FFA500"),
                Color.parseColor("#FF8888"),
                Color.parseColor("#AAAAFF"),
                Color.parseColor("#FFFFAA"),
                Color.parseColor("#55AAAA"),
                Color.parseColor("#AA33AA"),
                Color.parseColor("#0D0068")
        )
    }

    private class TrackedRecognition {
        var location: RectF? = null
        var detectionConfidence = 0f
        var color = 0
        var title: String = ""
    }

    private val screenRects: MutableList<Pair<Float?, RectF>> = LinkedList()
    private val trackedObjects: MutableList<TrackedRecognition> = LinkedList()
    private val boxPaint = Paint()
    private val borderedText: BorderedText
    private var frameToCanvasMatrix: Matrix? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var sensorOrientation = 0

    init {
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 10.0f
        boxPaint.strokeCap = Cap.ROUND
        boxPaint.strokeJoin = Join.ROUND
        boxPaint.strokeMiter = 100f
        val textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.resources.displayMetrics)
        borderedText = BorderedText(textSizePx)
    }

    fun setFrameConfiguration(
            width: Int, height: Int, sensorOrientation: Int) {
        frameWidth = width
        frameHeight = height
        this.sensorOrientation = sensorOrientation
    }

    fun trackResults(results: List<Detection>, timestamp: Long) {
        Log.v(TAG, String.format("Processing %d results from %d", results.size, timestamp))
        processResults(results)
    }

    fun draw(canvas: Canvas) {
        val rotated = sensorOrientation % 180 == 90
        val multiplier = min(
                canvas.height / (if (rotated) frameWidth else frameHeight).toFloat(),
                canvas.width / (if (rotated) frameHeight else frameWidth).toFloat())

        frameToCanvasMatrix = getTransformationMatrix(
                frameWidth,
                frameHeight,
                (multiplier * if (rotated) frameHeight else frameWidth).toInt(),
                (multiplier * if (rotated) frameWidth else frameHeight).toInt(),
                sensorOrientation,
                false)

        for (recognition in trackedObjects) {
            val trackedPos = RectF(recognition.location)
            frameToCanvasMatrix!!.mapRect(trackedPos)
            boxPaint.color = recognition.color

            val cornerSize = min(trackedPos.width(), trackedPos.height()) / 8.0f
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint)

            var labelString = ""
            if (!showConfidence && recognition.title.isNotBlank()) {
                labelString = recognition.title
            } else if (showConfidence && recognition.title.isNotBlank()) {
                labelString = "%s %.2f%%".format(recognition.title, 100 * recognition.detectionConfidence)
            } else if (showConfidence) {
                labelString = "%.2f%%".format(100 * recognition.detectionConfidence)
            }

            borderedText.drawText(
                    canvas, trackedPos.left + cornerSize, trackedPos.top, labelString, boxPaint)
        }
    }

    fun clear() {
        val rectsToTrack: MutableList<Pair<Float, Detection>> = LinkedList()
        screenRects.clear()

        trackedObjects.clear()
        if (rectsToTrack.isEmpty()) {
            Log.v(TAG, "Nothing to track, aborting.")
            return
        }

        for (potential in rectsToTrack) {
            val trackedRecognition = TrackedRecognition()
            trackedRecognition.detectionConfidence = potential.first
            trackedRecognition.location = RectF(potential.second.boundingBox)
            trackedRecognition.title = potential.second.className
            trackedRecognition.color = COLORS[trackedObjects.size]
            trackedObjects.add(trackedRecognition)
            if (trackedObjects.size >= COLORS.size) {
                break
            }
        }
    }

    private fun processResults(results: List<Detection>) {
        val rectsToTrack: MutableList<Pair<Float, Detection>> = LinkedList()
        screenRects.clear()

        val rgbFrameToScreen = Matrix(frameToCanvasMatrix)

        for (result in results) {
            val detectionFrameRect: RectF = result.boundingBox
            val detectionScreenRect = RectF()
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect)
            Log.v(TAG, "Result! Frame: ${result.boundingBox} mapped to screen:$detectionScreenRect")
            screenRects.add(Pair(result.confidence, detectionScreenRect))
            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                Log.w(TAG, "Degenerate rectangle! $detectionFrameRect")
                continue
            }
            rectsToTrack.add(Pair(result.confidence, result))
        }

        trackedObjects.clear()
        if (rectsToTrack.isEmpty()) {
            Log.v(TAG, "Nothing to track, aborting.")
            return
        }

        for (potential in rectsToTrack) {
            val trackedRecognition = TrackedRecognition()
            trackedRecognition.detectionConfidence = potential.first
            trackedRecognition.location = RectF(potential.second.boundingBox)
            trackedRecognition.title = potential.second.className
            trackedRecognition.color = COLORS[trackedObjects.size]
            trackedObjects.add(trackedRecognition)
            if (trackedObjects.size >= COLORS.size) {
                break
            }
        }
    }

}