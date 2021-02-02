package pl.dps.mobile_app.helpers

import android.content.SharedPreferences
import pl.dps.detector.enums.DetectionModel

class DrivepalOptions(prefs: SharedPreferences? = null) {

    var diagnosticsEnabled: Boolean = false
    var testSpeed: Boolean = false
    var showBoundingBoxes: Boolean = false

    var pedestrianSpeedLimit: Float = 40.0f
    var personSpeedLimit: Float = 40.0f
    var stopSpeedLimit: Float = 40.0f

    var model: DetectionModel = DetectionModel.YOLO_V4_TINY_512_DRIVEPAL
    var useRemoteDetector: Boolean = false

    var diagnosticServerUrl: String = "http://server.drivepal.pl:5000"
    var detectorServerUrl: String = "http://server.drivepal.pl:5001"

    init {
        update(prefs)
    }

    fun update(prefs: SharedPreferences?) {
        if (prefs == null) {
            return
        }

        diagnosticsEnabled = prefs.getBoolean("diagnostics", false)
        testSpeed = prefs.getBoolean("testSpeed", false)
        showBoundingBoxes = prefs.getBoolean("boundingBoxes", false)

        pedestrianSpeedLimit =
                prefs.getInt("pedestrianCrossingSpeedLimit", 40).toFloat()
        personSpeedLimit = prefs.getInt("personSpeedLimit", 40).toFloat()
        stopSpeedLimit = prefs.getInt("stopSpeedLimit", 40).toFloat()

        val modelStr = prefs.getString("model", "YOLO_V4_TINY_512_DRIVEPAL")
        model = DetectionModel.valueOf(modelStr!!)
        useRemoteDetector = prefs.getBoolean("useRemoteDetector", false)

        diagnosticServerUrl =
                prefs.getString("model", "http://server.drivepal.pl:5000")!!
        detectorServerUrl =
                prefs.getString("model", "http://server.drivepal.pl:5001")!!
    }

}