package pl.dps.mobile_app.helpers

import android.graphics.*
import android.location.Location
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.example.mobile_app.R
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

class DetectionProcessor(private val activity: AppCompatActivity,
                         private val detector: Detector,
                         private val options: DrivepalOptions,
                         private val sendDetection: (JSONObject) -> Unit) {

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
    private lateinit var trackingOverlay: OverlayView
    var tracker: MultiBoxTracker? = null

    /* Camera preview variables */
    private var previewView: PreviewView = activity.findViewById(R.id.viewFinder)
    private var previewWidth = 0
    private var previewHeight = 0

    suspend fun initializeTrackingLayout(cropSize: Int, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP,
                activity.resources.displayMetrics
        )

        val borderedText = BorderedText(textSizePx)

        borderedText.setTypeface(Typeface.MONOSPACE)

        if (options.showBoundingBoxes) {
            tracker = MultiBoxTracker(activity, showConfidence = false)
        }

        previewWidth = previewView.width
        previewHeight = previewView.height

        while (previewWidth == 0 || previewHeight == 0) {
            delay(200)
            previewWidth = previewView.width
            previewHeight = previewView.height
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

        if (options.showBoundingBoxes) {
            trackingOverlay = activity.findViewById(R.id.tracking_overlay)
            trackingOverlay.removeCallbacks { }

            trackingOverlay.addCallback(object : OverlayView.DrawCallback {
                override fun drawCallback(canvas: Canvas) {
                    tracker!!.draw(canvas)
                }
            })
            tracker!!.setFrameConfiguration(previewWidth, previewHeight, rotation)
        }
    }

    fun processImage(bitmap: Bitmap,
                     imageStamp: Long,
                     currentLocation: Location?,
                     sendDiagnostics: Boolean): Long {

        if (options.showBoundingBoxes) {
            trackingOverlay.postInvalidate()
        }

        Log.i(TAG, "Running detection on image $imageStamp")
        val startTime = SystemClock.uptimeMillis()
        val results = detector.recognizeImage(bitmap, returnImage = sendDiagnostics)
        val inferenceTime: Long = SystemClock.uptimeMillis() - startTime

        Log.i(TAG, "Recognized objects : " + results.detections.size)
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
                showAlertForPerson(boundingBox, currentLocation)
            } else if (className.contains("Sign speed")) {
                val tempSpeed = className.split(" ").last().toFloat()
                if (tempSpeed > maxSpeedLimit) {
                    maxSpeedLimit = tempSpeed
                }
            } else if (className in arrayOf("Sign stop", "Sign traffic ban", "Sign no entry")) {
                showAlertForStopSign(currentLocation)
            } else if (className in arrayOf("Sign pedestrian crossing", "Sign children")) {
                showAlertForPedestrianCrossingSign(currentLocation)
            }
        }
        if (maxSpeedLimit >= 0.0f) {
            showAlertForSpeedSign(currentLocation, maxSpeedLimit)
        }

        if (options.showBoundingBoxes) {
            tracker!!.trackResults(results.detections, imageStamp)
            trackingOverlay.postInvalidate()
        }

        return inferenceTime
    }

    private fun sendResults(results: DetectionResults) {
        val resultsJson = results.toJSON()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                resultsJson.put("modelName", detector.getDetectionModel().toString())
                sendDetection(resultsJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAlertForPerson(boundingBox: RectF, location: Location?) {
        val value = abs(0.5 - boundingBox.centerX() / previewWidth)
        val speedCondition = location == null || location.speed >= options.personSpeedLimit

        if (value < 0.1F && speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("Person on the road. Slow down!")
            }
        }
    }

    private fun showAlertForStopSign(location: Location?) {
        val speedCondition = location == null || location.speed >= options.stopSpeedLimit

        if (speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("Stop sign detected. Stop your vehicle!")
            }
        }
    }

    private fun showAlertForPedestrianCrossingSign(location: Location?) {
        val speedCondition = location == null || location.speed >= options.pedestrianSpeedLimit

        if (speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("Possible pedestrians. Slow down!")
            }
        }
    }

    private fun showAlertForSpeedSign(location: Location?, speedLimit: Float) {
        val speedCondition = location == null || location.speed >= speedLimit

        if (speedCondition && alert == null) {
            CoroutineScope(Dispatchers.Main).launch {
                showAlert("You're driving too fast. Slow down!")
            }
        }
    }

    private suspend fun showAlert(message: String) {
        if (alert == null) {
            alert = Alert(activity, message)
            alert!!.show()
            delay(ALERT_TIMEOUT)
            alert!!.hide()
            alert = null
        }
    }

}