package pl.dps.detector.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.os.SystemClock
import android.renderscript.*
import android.util.Base64
import android.util.Log
import pl.dps.detector.RemoteDetector
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Utility class for manipulating images.
 */
object ImageUtils {
    private const val TAG = "ImageUtils"

    private val intValuesMap = hashMapOf<Int, IntArray>()

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    fun convertBitmapToIntByteBuffer(bitmap: Bitmap, inputSize: Int, byteBuffer: ByteBuffer) {
        val startTime = SystemClock.uptimeMillis()
        val intValuesSize = inputSize * inputSize
        if (!intValuesMap.containsKey(intValuesSize)) {
            intValuesMap[intValuesSize] = IntArray(intValuesSize)
        }
        val intValues = intValuesMap[inputSize * inputSize]!!

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        scaledBitmap.recycle()

        byteBuffer.clear()
        for (pixel in intValues) {
            val r = (pixel and 0xFF).toByte()
            val g = (pixel shr 8 and 0xFF).toByte()
            val b = (pixel shr 16 and 0xFF).toByte()

            byteBuffer.put(g)
            byteBuffer.put(r)
            byteBuffer.put(b)
        }

        Log.v(RemoteDetector.TAG, "ByteBuffer conversion time : ${SystemClock.uptimeMillis() - startTime} ms")
    }

    fun encodeToBase64(image: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes: ByteArray = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Returns a transformation matrix from one reference frame into another. Handles cropping (if
     * maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth            Width of source frame.
     * @param srcHeight           Height of source frame.
     * @param dstWidth            Width of destination frame.
     * @param dstHeight           Height of destination frame.
     * @param applyRotation       Amount of rotation to apply from one frame to another. Must be a multiple
     * of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     * cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    fun getTransformationMatrix(
            srcWidth: Int,
            srcHeight: Int,
            dstWidth: Int,
            dstHeight: Int,
            applyRotation: Int,
            maintainAspectRatio: Boolean): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                Log.w(TAG, String.format("Rotation of %d mod 90 != 0", applyRotation))
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

            // Rotate around origin.
            matrix.postRotate(applyRotation.toFloat())
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        val transpose = (abs(applyRotation) + 90) % 180 == 0
        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()
            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                val scaleFactor = scaleFactorX.coerceAtLeast(scaleFactorY)
                matrix.postScale(scaleFactor, scaleFactor)
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }
        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }

    private var renderScript: RenderScript? = null
    private lateinit var scriptYuvToRgb: ScriptIntrinsicYuvToRGB

    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation

    fun imageToBitmap(image: Image, context: Context?): Bitmap {
        val yuvBuffer: ByteArray = yuv420ToByteArray(image)

        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)

        if (renderScript == null) {
            renderScript = RenderScript.create(context)
            scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_3(renderScript))

            val elemType = Type.Builder(renderScript, Element.YUV(renderScript))
                    .setYuvFormat(ImageFormat.YUV_420_888)
                    .create()

            inputAllocation = Allocation.createSized(renderScript, elemType.element, yuvBuffer.size)
            outputAllocation = Allocation.createFromBitmap(renderScript, bitmap)
        }

        inputAllocation.copyFrom(yuvBuffer)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(bitmap)

        return bitmap
    }

    private fun yuv420ToByteArray(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer.toByteArray()
        val uBuffer = image.planes[1].buffer.toByteArray()
        val vBuffer = image.planes[2].buffer.toByteArray()

        return yBuffer + uBuffer + vBuffer
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

}