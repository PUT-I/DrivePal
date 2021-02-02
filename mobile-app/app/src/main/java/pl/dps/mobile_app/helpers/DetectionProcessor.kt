package pl.dps.mobile_app.helpers

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import androidx.camera.view.PreviewView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.dps.detector.DetectionResults
import pl.dps.detector.Detector
import pl.dps.detector.utils.ImageUtils
import pl.dps.detector.visualization.BorderedText
import pl.dps.detector.visualization.MultiBoxTracker
import pl.dps.detector.visualization.OverlayView
import kotlin.math.abs

class DetectionProcessor(private var context: Context?,
                         private var detector: Detector?,
                         private var options: DrivepalOptions?,
                         private var previewView: PreviewView?,
                         private var trackingOverlay: OverlayView?,
                         private var sendDetection: (JSONObject) -> Unit) {

    companion object {
        private const val TAG = "DetectionProcessor"

        private const val MAINTAIN_ASPECT = false
        private const val TEXT_SIZE_DIP = 10f
        private const val ALERT_TIMEOUT: Long = 1000L
    }

    /* Alert variables */
    private var alert: Alert? = null

    /* Tracking overlay variables */
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
    var tracker: MultiBoxTracker? = null

    /* Camera preview variables */
    private var previewWidth = 0
    private var previewHeight = 0

    fun release() {
        context = null
        detector = null
        options = null
        previewView = null
        trackingOverlay = null

        alert = null
        tracker = null
    }

    suspend fun initializeTrackingLayout(cropSize: Int, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP,
                context!!.resources.displayMetrics
        )

        val borderedText = BorderedText(textSizePx)

        borderedText.setTypeface(Typeface.MONOSPACE)

        if (options!!.showBoundingBoxes) {
            tracker = MultiBoxTracker(context!!, showConfidence = false)
        }

        previewWidth = previewView!!.width
        previewHeight = previewView!!.height

        while (previewWidth == 0 || previewHeight == 0) {
            delay(200)
            previewWidth = previewView!!.width
            previewHeight = previewView!!.height
        }

        Log.i(TAG, String.format("Camera orientation relative to screen canvas: %d", rotation))
        Log.i(TAG, String.format("Initializing at size %dx%d", previewWidth, previewHeight))

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
        frameToCropTransform = ImageUtils.getTransformationMatrix(
                previewWidth, previewHeight,
                cropSize, cropSize,
                rotation, MAINTAIN_ASPECT)
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)

        if (options!!.showBoundingBoxes) {
            trackingOverlay!!.addCallback(object : OverlayView.DrawCallback {
                override fun drawCallback(canvas: Canvas) {
                    tracker?.draw(canvas)
                }
            })
            tracker?.setFrameConfiguration(previewWidth, previewHeight, rotation)
        }
    }

    fun processImage(bitmap: Bitmap,
                     imageStamp: Long,
                     speed: Float,
                     sendDiagnostics: Boolean): Long {

        if (options!!.showBoundingBoxes) {
            trackingOverlay!!.postInvalidate()
        }

        Log.v(TAG, "Running detection on image $imageStamp")
        val startTime = SystemClock.uptimeMillis()
        val results = detector!!.recognizeImage(bitmap, returnImage = sendDiagnostics)
        val inferenceTime: Long = SystemClock.uptimeMillis() - startTime

        Log.v(TAG, "Recognized objects : " + results.detections.size)
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)

        val canvas = Canvas(cropCopyBitmap!!)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f

        if (sendDiagnostics && results.detections.isNotEmpty()) {
            sendResults(results)
        }

        var maxSpeedLimit = -1.0f
        for (result in results.detections) {
            val boundingBox: RectF = result.boundingBox
            canvas.drawRect(boundingBox, paint)
            cropToFrameTransform!!.mapRect(boundingBox)
            result.boundingBox = boundingBox

            val className = result.className

            if (className == "Person") {
                showAlertForPerson(boundingBox, speed)
            } else if (className.contains("Sign speed")) {
                val tempSpeed = className.split(" ").last().toFloat()
                if (tempSpeed > maxSpeedLimit) {
                    maxSpeedLimit = tempSpeed
                }
            } else if (className in arrayOf("Sign stop", "Sign traffic ban", "Sign no entry")) {
                showAlertForStopSign(speed)
            } else if (className in arrayOf("Sign pedestrian crossing", "Sign children")) {
                showAlertForPedestrianCrossingSign(speed)
            }
        }
        if (maxSpeedLimit >= 0.0f) {
            showAlertForSpeedSign(speed, maxSpeedLimit)
        }

        if (options!!.showBoundingBoxes) {
            tracker?.trackResults(results.detections, imageStamp)
            trackingOverlay?.postInvalidate()
        }

        return inferenceTime
    }

    private fun sendResults(results: DetectionResults) {
        val resultsJson = results.toJSON()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                resultsJson.put("modelName", detector!!.getDetectionModel().toString())
                sendDetection(resultsJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAlertForPerson(boundingBox: RectF, speed: Float) {
        val value = abs(0.5 - boundingBox.centerX() / previewWidth)
        val speedCondition = speed < 0.0f || speed >= options!!.personSpeedLimit

        if (value < 0.1F && speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("Person on the road. Slow down!")
            }
        }
    }

    private fun showAlertForStopSign(speed: Float) {
        val speedCondition = speed < 0.0f || speed >= options!!.stopSpeedLimit

        if (speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("Stop sign detected. Stop your vehicle!")
            }
        }
    }

    private fun showAlertForPedestrianCrossingSign(speed: Float) {
        val speedCondition = speed < 0.0f || speed >= options!!.pedestrianSpeedLimit

        if (speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("Possible pedestrians. Slow down!")
            }
        }
    }

    private fun showAlertForSpeedSign(speed: Float, speedLimit: Float) {
        val speedCondition = speed < 0.0f || speed >= speedLimit

        if (speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("You're driving too fast. Slow down!")
            }
        }
    }

    private suspend fun showAlert(message: String) {
        if (alert == null) {
            alert = Alert(context!!, message)
            alert!!.show()
            delay(ALERT_TIMEOUT)
            alert!!.hide()
            alert = null
        }
    }

}