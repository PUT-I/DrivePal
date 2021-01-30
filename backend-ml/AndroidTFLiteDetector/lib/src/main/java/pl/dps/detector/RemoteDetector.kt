package pl.dps.detector

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import pl.dps.detector.Detector.Detection
import pl.dps.detector.enums.DetectionModel
import pl.dps.detector.utils.ImageUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.max
import kotlin.math.min


@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
internal class RemoteDetector(assetManager: AssetManager,
                              private val queue: RequestQueue,
                              private val url: String,
                              private val detectionModel: DetectionModel,
                              private val minimumConfidence: Float) : Detector {

    companion object {
        const val TAG = "ServerDetector"
    }

    private val inputSize: Int = detectionModel.inputSize

    // Config values.
    // Pre-allocated buffers.
    private val labels: MutableList<String> = ArrayList()
    private var imgData: ByteBuffer
    private val intValues = IntArray(inputSize * inputSize)

    // input size * input size * pixel size
    private var byteBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3)

    init {
        Log.d(TAG, "Initializing remote detector")
        Log.d(TAG, "URL : $url")
        Log.d(TAG, "Detection model : $detectionModel")
        Log.d(TAG, "Minimum confidence : $minimumConfidence")

        byteBuffer.order(ByteOrder.nativeOrder())

        val actualFilename = detectionModel.labelFilePath
                .split("file:///android_asset/").toTypedArray()[1]
        val labelsInput = assetManager.open(actualFilename)
        val br = BufferedReader(InputStreamReader(labelsInput))
        var line: String?
        while (br.readLine().also { line = it } != null) {
            if (line == null) {
                break
            }

            Log.w(TAG, line!!)
            labels.add(line!!)
        }
        br.close()

        imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())

        // Test connection
        Log.d(TAG, "Sending test request : $url/test")
        val future: RequestFuture<String> = RequestFuture.newFuture()
        val request = StringRequest(Request.Method.GET, "$url/test", future, future)

        queue.add(request)

        val response: String = future.get(5, TimeUnit.SECONDS)
        if (response.toLowerCase(Locale.ROOT) != "ok") {
            throw ConnectException("Could not connect with remote detector (received: ${response})")
        }
    }

    override fun getDetectionModel(): DetectionModel {
        return detectionModel
    }

    override fun recognizeImage(bitmap: Bitmap, returnImage: Boolean): DetectionResults {
        ImageUtils.convertBitmapToIntByteBuffer(bitmap, inputSize, byteBuffer)

        return getDetections(bitmap, returnImage)
    }

    private fun getDetections(bitmap: Bitmap, returnImage: Boolean): DetectionResults {
        val base64String = Base64.getEncoder()
                .encodeToString(byteBuffer.array().sliceArray(0 until byteBuffer.capacity()))

        val jsonBody = JSONObject()
        jsonBody.run {
            put("detectionModel", detectionModel.name)
            put("scoreThreshold", minimumConfidence)
            put("iouThreshold", 0.5f)
            put("image", base64String)
        }

        val future: RequestFuture<JSONObject> = RequestFuture.newFuture()
        val request = JsonObjectRequest(Request.Method.POST, "$url/detect", jsonBody, future, future)

        queue.add(request)

        var response: JSONObject? = null

        try {
            response = future.get(5, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Log.w(TAG, "Detection request to $url timed out.")
        } catch (e: InterruptedException) {
            Log.i(TAG, "Detection request interrupted")
        } catch (e: ExecutionException) {
            Log.i(TAG, "Detection request execution exception")
        }

        if (response == null) {
            return DetectionResults()
        }

        val results = DetectionResults()
        if (returnImage) {
            results.image = ImageUtils.encodeToBase64(bitmap)
        }

        val scores = response.getJSONArray("scores")
        val bboxes = response.getJSONArray("bboxes")

        for (i in 0 until scores.length()) {
            var maxClass = 0.0
            var detectedClass = -1
            val classes = scores.getJSONArray(i)
            for (c in labels.indices) {
                if (classes[c] as Double > maxClass) {
                    detectedClass = c
                    maxClass = classes[c] as Double
                }
            }

            val score = maxClass.toFloat()
            val bbox = bboxes.getJSONArray(i)

            val xPos = (bbox[0] as Double).toFloat()
            val yPos = (bbox[1] as Double).toFloat()
            val w = (bbox[2] as Double).toFloat()
            val h = (bbox[3] as Double).toFloat()
            val rectF = RectF(
                    max(0f, xPos - w / 2),
                    max(0f, yPos - h / 2),
                    min(bitmap.width - 1.toFloat(), xPos + w / 2),
                    min(bitmap.height - 1.toFloat(), yPos + h / 2))

            results.detections.add(Detection("" + i, labels[detectedClass], score, rectF, detectedClass))
        }

        return results
    }

}