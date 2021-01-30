package pl.dps.mobile_app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.mobile_app.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.dps.detector.Detector
import pl.dps.detector.DetectorFactory
import pl.dps.detector.utils.ImageUtils
import pl.dps.mobile_app.helpers.DetectionProcessor
import pl.dps.mobile_app.helpers.DrivepalOptions
import pl.dps.mobile_app.helpers.ServerClient
import pl.dps.mobile_app.helpers.Utils

class DetectorActivity : AppCompatActivity(), LocationListener {
    companion object {
        private const val TAG = "DetectorActivity"

        private const val MINIMUM_CONFIDENCE = 0.7f
    }

    private var options: DrivepalOptions = DrivepalOptions()

    /* Alert variables */
    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private lateinit var speedText: TextView

    /* Image analysis variables */
    private var detector: Detector? = null
    private var imageStamp: Long = 0

    /* Camera preview variables */
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView

    /* Diagnostics variables */
    private lateinit var queue: RequestQueue
    private var soc = "UNKNOWN"
    private var deviceId: String? = null
    private lateinit var serverClient: ServerClient

    /* Detector variables */
    private var detectionProcessor: DetectionProcessor? = null

    /* Lifecycle methods */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        queue = Volley.newRequestQueue(this)
        serverClient = ServerClient(queue, options.diagnosticServerUrl)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_camera)

        speedText = findViewById(R.id.text_speed)
        speedText.text = getString(R.string.speed_text).format(0.0f)

        previewView = findViewById(R.id.viewFinder)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        requestPermissions(arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ), 1)

        @SuppressLint("HardwareIds")
        deviceId = "DEVICE_" +
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        soc = Utils.getSocModel()

        Log.d(TAG, "cpu: $soc")
    }

    override fun onResume() {
        super.onResume()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        options = DrivepalOptions(prefs)

        val cropSize = initializeDetector()
        lifecycleScope.launch(Dispatchers.Main) {
            detectionProcessor = DetectionProcessor(this@DetectorActivity,
                    detector!!,
                    options,
                    serverClient::sendDetection)

            detectionProcessor!!.initializeTrackingLayout(cropSize, Surface.ROTATION_90)
        }
    }

    /* Action bar methods */

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_detector, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        return true
    }

    /* Listener functions */

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == 1) {
            val indexOfCameraPermission = permissions.indexOf(Manifest.permission.CAMERA)
            if (grantResults[indexOfCameraPermission] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
                startLocationManager()
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

    private var testSpeedValue: Float = 100.0f
    private var testSpeedSlowdown: Boolean = false

    override fun onLocationChanged(location: Location) {
        if (options.testSpeed) {
            if (testSpeedSlowdown) {
                testSpeedValue -= 10.0f
            } else {
                testSpeedValue += 10.0f
            }

            testSpeedSlowdown = testSpeedValue >= 150.0f

            location.speed = testSpeedValue
        } else {
            location.speed *= 3.6f
        }
        currentLocation = location
        speedText.text = getString(R.string.speed_text).format(location.speed)
    }

    /* Other methods */

    private fun startLocationManager() {
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

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

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ::analyzeImage)

        cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
        )
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun analyzeImage(image: ImageProxy) {
        CoroutineScope(Dispatchers.IO).launch {
            imageStamp++
            val startTime = SystemClock.uptimeMillis()
            val bmp = ImageUtils.imageToBitmap(image.image!!, applicationContext)
            Log.i(TAG, "Conversion time : ${SystemClock.uptimeMillis() - startTime} ms")

            val sendDiagnostics: Boolean = options.diagnosticsEnabled && imageStamp % 10 == 0L

            val inferenceTime = detectionProcessor!!
                    .processImage(bmp, imageStamp, currentLocation, sendDiagnostics)

            val processingTime = SystemClock.uptimeMillis() - startTime

            if (sendDiagnostics) {
                val myJson = JSONObject()
                myJson.run {
                    put("deviceId", deviceId ?: JSONObject.NULL)
                    put("cpuTemperature", Utils.getCpuTemperature() ?: JSONObject.NULL)
                    put("inferenceTime", inferenceTime)
                    put("modelName", options.model.toString())
                    put("processingTime", processingTime)
                    put("soc", soc)
                    put("source", "android")
                    put("version", 1)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    serverClient.sendDiagnostics(myJson)
                }
            }

            image.close() // Without this line analyzer fires only once
        }
    }

    private fun initializeDetector(): Int {
        if (options.useRemoteDetector) {
            try {
                detector = DetectorFactory.createDetector(assets,
                        queue,
                        options.detectorServerUrl,
                        options.model,
                        MINIMUM_CONFIDENCE)
                return options.model.inputSize
            } catch (exception: Exception) {
                Log.e(TAG, "Could not initialize remote detector")
                Log.e(TAG, exception.stackTraceToString())
                val toast = Toast.makeText(this,
                        "Remote detector could not be initialized",
                        Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        detector = DetectorFactory.createDetector(assets, options.model, MINIMUM_CONFIDENCE)

        return options.model.inputSize
    }

}
