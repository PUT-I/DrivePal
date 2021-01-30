package pl.dps.detector.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Surface
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import pl.dps.detector.Detector
import pl.dps.detector.DetectorFactory
import pl.dps.detector.ImageUtils
import pl.dps.detector.ImageUtils.getTransformationMatrix
import pl.dps.detector.enums.DetectionModel
import pl.dps.detector.visualization.BorderedText
import pl.dps.detector.visualization.MultiBoxTracker
import pl.dps.detector.visualization.OverlayView
import pl.dps.detector.visualization.OverlayView.DrawCallback
import java.io.IOException

open class DetectorActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DetectorActivity"

        private const val MINIMUM_CONFIDENCE = 0.3f
        private const val MAINTAIN_ASPECT = false
        private const val TEXT_SIZE_DIP = 10f
        private const val IS_DEBUG = false
    }

    private var sensorOrientation: Int? = null
    private var detector: Detector? = null
    private var cropSize: Int = 0

    private var trackingOverlay: OverlayView? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
    private var tracker: MultiBoxTracker? = null
    private var imageStamp: Long = 0

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView

    private var previewWidth = 0
    private var previewHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.viewFinder)

        cropSize = initializeDetector(DetectionModel.YOLO_V4_TINY_512_DRIVEPAL)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (requestCode == 1) {
            val indexOfCameraPermission = permissions.indexOf(Manifest.permission.CAMERA)
            if (grantResults[indexOfCameraPermission] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                        this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { image: ImageProxy ->
            CoroutineScope(Dispatchers.IO).launch {
                val startTime = SystemClock.uptimeMillis()
                val bmp = ImageUtils.imageToBitmap(image.image!!, applicationContext)
                Log.i(TAG, "Conversion time : ${SystemClock.uptimeMillis() - startTime} ms")
                processImage(bmp)
                image.close() // Without this line analyzer fires only once
                Log.i(TAG, "Processing time : ${SystemClock.uptimeMillis() - startTime} ms")
            }
        })

        initializeTrackingLayout(cropSize, Surface.ROTATION_90)

        cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
        )
    }

    private fun initializeTrackingLayout(cropSize: Int, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics)
        val borderedText = BorderedText(textSizePx)

        borderedText.setTypeface(Typeface.MONOSPACE)
        tracker = MultiBoxTracker(this)

        previewWidth = previewView.width
        previewHeight = previewView.height
        sensorOrientation = rotation

        Log.i(TAG, String.format("Camera orientation relative to screen canvas: %d", sensorOrientation))
        Log.i(TAG, String.format("Initializing at size %dx%d", previewWidth, previewHeight))

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
        frameToCropTransform = getTransformationMatrix(
                previewWidth, previewHeight,
                cropSize, cropSize,
                sensorOrientation!!, MAINTAIN_ASPECT)
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
        trackingOverlay = findViewById(R.id.tracking_overlay)

        trackingOverlay!!.addCallback(object : DrawCallback {
            override fun drawCallback(canvas: Canvas?) {
                tracker!!.draw(canvas!!)
                if (IS_DEBUG) {
                    tracker!!.drawDebug(canvas)
                }
            }
        })
        tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)

        MainScope().launch {
            findViewById<TextView>(R.id.frame_info).text = "${previewWidth}x$previewHeight"
            findViewById<TextView>(R.id.crop_info).text = "${cropSize}x$cropSize"
        }
    }

    private fun initializeDetector(detectionModel: DetectionModel): Int {
        try {
            detector = DetectorFactory.createDetector(assets, detectionModel, MINIMUM_CONFIDENCE)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Exception initializing detector!")
            Log.e(TAG, e.toString())
            val toast = Toast.makeText(
                    applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT)
            toast.show()
            finish()
        }
        return detectionModel.inputSize
    }

    private fun processImage(bitmap: Bitmap) {
        ++imageStamp
        trackingOverlay!!.postInvalidate()

        Log.i(TAG, "Running detection on image $imageStamp")
        val startTime = SystemClock.uptimeMillis()
        val results = detector!!.recognizeImage(bitmap)
        val inferenceTime = SystemClock.uptimeMillis() - startTime
        Log.i(TAG, "Inference time : $inferenceTime ms")

        MainScope().launch {
            findViewById<TextView>(R.id.inference_info).text = "$inferenceTime ms"
        }

        Log.i(TAG, "Recognized objects : ${results.detections.size}")
        if (results.detections.isEmpty()) {
            tracker!!.trackResults(results.detections, imageStamp)
            trackingOverlay!!.postInvalidate()
            return
        }

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)

        val canvas = Canvas(cropCopyBitmap!!)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f

        for (result in results.detections) {
            val location: RectF = result.location

            canvas.drawRect(location, paint)
            cropToFrameTransform!!.mapRect(location)
        }

        CoroutineScope(Dispatchers.Main).launch {
            tracker!!.trackResults(results.detections, imageStamp)
            trackingOverlay!!.postInvalidate()
        }
    }

}