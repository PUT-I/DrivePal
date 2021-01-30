package pl.dps.detector

import android.content.res.AssetManager
import com.android.volley.RequestQueue
import pl.dps.detector.enums.DetectionModel

object DetectorFactory {

    fun createDetector(assetManager: AssetManager,
                       detectionModel: DetectionModel,
                       minimumConfidence: Float): Detector {
        return YoloV4Detector(assetManager, detectionModel, minimumConfidence)
    }

    fun createDetector(assetManager: AssetManager,
                       queue: RequestQueue,
                       url: String,
                       detectionModel: DetectionModel,
                       minimumConfidence: Float): Detector {
        return RemoteDetector(assetManager, queue, url, detectionModel, minimumConfidence)
    }

}
