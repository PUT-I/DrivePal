package pl.dps.detector

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class DetectionResults {

    var image: String = ""
    var imageSize: Int = 0
    var detections = ArrayList<Detector.Detection>()

    fun toJSON(): JSONObject {
        val detectionsTemp = mutableListOf<Detector.Detection>()

        // Normalizes box location
        for (detection in detections) {
            detection.boundingBox.left /= imageSize
            detection.boundingBox.top /= imageSize
            detection.boundingBox.right /= imageSize
            detection.boundingBox.bottom /= imageSize
            detectionsTemp.add(detection)
        }

        val jsonDetections = JSONArray()
        for (detection in detectionsTemp) {
            jsonDetections.put(detection.toJSON())
        }

        val result = JSONObject()
        result.put("image", image)
        result.put("detections", jsonDetections)
        return result
    }

}