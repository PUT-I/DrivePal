package pl.dps.detector

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import pl.dps.detector.Detector.Detection
import pl.dps.detector.enums.DetectionModel
import pl.dps.detector.utils.ImageUtils
import pl.dps.detector.utils.Utils.loadModelFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.max
import kotlin.math.min


@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
internal class YoloV4Detector(assetManager: AssetManager,
                              private val detectionModel: DetectionModel,
                              private val minimumConfidence: Float) : Detector {

    companion object {
        const val TAG = "YoloV4Detector"
        const val NUM_THREADS = 4
    }

    private val inputSize: Int = detectionModel.inputSize

    // Config values.
    // Pre-allocated buffers.
    private val labels: MutableList<String> = ArrayList()
    private var imgData: ByteBuffer
    private val interpreter: Interpreter
    private val mNmsThresh = 0.6f

    private val intValues = IntArray(inputSize * inputSize)

    // input size * input size * pixel count * pixel size
    private var byteBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
    private var intByteBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3)

    private val outputMap: MutableMap<Int, Any> = HashMap()

    init {
        byteBuffer.order(ByteOrder.nativeOrder())
        intByteBuffer.order(ByteOrder.nativeOrder())

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
        interpreter = initializeInterpreter(assetManager)

        imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())

        outputMap[0] = Array(1) { Array(detectionModel.outputSize) { FloatArray(4) } }
        outputMap[1] = Array(1) { Array(detectionModel.outputSize) { FloatArray(labels.size) } }
    }

    override fun getDetectionModel(): DetectionModel {
        return detectionModel
    }

    override fun recognizeImage(bitmap: Bitmap, returnImage: Boolean): DetectionResults {
        convertBitmapToByteBuffer(bitmap)
        val results = getDetections(byteBuffer, bitmap)

        if (returnImage) {
            // RGBA to ARGB
            for (i in intValues.indices) {
                val pixel = intValues[i]

                val r = (pixel and 0xFF)
                val g = (pixel shr 8 and 0xFF)
                val b = (pixel shr 16 and 0xFF)
                val newPixel = (255 shl 24) + (r shl 16) + (g shl 8) + b

                intValues[i] = newPixel
            }

            val shiftedBitmap =
                    Bitmap.createBitmap(intValues, 0, inputSize, inputSize, inputSize, Bitmap.Config.ARGB_8888)

            val encodedImage = ImageUtils.encodeToBase64(shiftedBitmap)
            results.image = encodedImage
            results.imageSize = inputSize
        }

        return nms(results) // Recognitions
    }

    private fun initializeInterpreter(assetManager: AssetManager): Interpreter {
        return try {
            val options = Interpreter.Options()
            options.setNumThreads(NUM_THREADS)
            options.setUseXNNPACK(true)
            Interpreter(loadModelFile(assetManager, detectionModel.modelFilename), options)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        val startTime = SystemClock.uptimeMillis()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        byteBuffer.clear()
        for (pixel in intValues) {
            val r = (pixel and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel shr 16 and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        Log.i(TAG, "ByteBuffer conversion time : ${SystemClock.uptimeMillis() - startTime} ms")
    }

    private fun getDetections(byteBuffer: ByteBuffer, bitmap: Bitmap?): DetectionResults {
        interpreter.runForMultipleInputsOutputs(arrayOf(byteBuffer), outputMap)

        @Suppress("UNCHECKED_CAST")
        val boundingBoxes = outputMap[0] as Array<Array<FloatArray>>

        @Suppress("UNCHECKED_CAST")
        val outScore = outputMap[1] as Array<Array<FloatArray>>

        val results = DetectionResults()

        for (i in 0 until detectionModel.outputSize) {
            var maxClass = 0f
            var detectedClass = -1
            val classes = outScore[0][i]
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }

            val score = maxClass
            if (score > minimumConfidence) {
                val xPos = boundingBoxes[0][i][0]
                val yPos = boundingBoxes[0][i][1]
                val w = boundingBoxes[0][i][2]
                val h = boundingBoxes[0][i][3]
                val rectF = RectF(
                        max(0f, xPos - w / 2),
                        max(0f, yPos - h / 2),
                        min(bitmap!!.width - 1.toFloat(), xPos + w / 2),
                        min(bitmap.height - 1.toFloat(), yPos + h / 2))

                results.detections
                        .add(Detection(i.toString(), labels[detectedClass], score, rectF, detectedClass))
            }
        }
        return results
    }

    //non maximum suppression
    private fun nms(results: DetectionResults): DetectionResults {
        val list = results.detections
        val nmsList = ArrayList<Detection>()

        for (k in labels.indices) {
            //1.find max confidence per class
            val priorityQueue = PriorityQueue(50) { lhs: Detection, rhs: Detection ->
                (rhs.confidence!!).compareTo(lhs.confidence!!)
            }

            for (i in list.indices) {
                if (list[i].detectedClass == k) {
                    priorityQueue.add(list[i])
                }
            }

            //2.do non maximum suppression
            while (priorityQueue.size > 0) {
                //insert detection with max confidence
                val a = arrayOfNulls<Detection>(priorityQueue.size)
                val detections: Array<Detection> = priorityQueue.toArray(a)
                val max = detections[0]
                nmsList.add(max)
                priorityQueue.clear()
                for (j in detections.indices) {
                    val detection = detections[j]

                    if (boxIoU(max.boundingBox, detection.boundingBox) < mNmsThresh) {
                        priorityQueue.add(detection)
                    }
                }
            }
        }

        results.detections = nmsList
        return results
    }

    // Box Intersection over Union
    private fun boxIoU(a: RectF, b: RectF): Float {
        return boxIntersection(a, b) / boxUnion(a, b)
    }

    private fun boxIntersection(a: RectF, b: RectF): Float {
        val w = overlap((a.left + a.right) / 2, a.right - a.left,
                (b.left + b.right) / 2, b.right - b.left)

        val h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
                (b.top + b.bottom) / 2, b.bottom - b.top)

        // Returns area
        return if (w < 0F || h < 0F) 0F else w * h
    }

    private fun boxUnion(a: RectF, b: RectF): Float {
        val i = boxIntersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    private fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
        val l1 = x1 - w1 / 2
        val l2 = x2 - w2 / 2
        val left = max(l1, l2)

        val r1 = x1 + w1 / 2
        val r2 = x2 + w2 / 2
        val right = min(r1, r2)

        return right - left
    }

}