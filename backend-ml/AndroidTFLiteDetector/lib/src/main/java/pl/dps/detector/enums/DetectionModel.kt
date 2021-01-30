package pl.dps.detector.enums

enum class DetectionModel(val modelFilename: String,
                          val labelFilePath: String,
                          val inputSize: Int,
                          val outputSize: Int) {

    YOLO_V4_TINY_384_DRIVEPAL("yolov4-tiny-384-drivepal.tflite",
            "file:///android_asset/drivepal-labels.txt",
            384,
            2160),
    YOLO_V4_TINY_512_DRIVEPAL("yolov4-tiny-512-drivepal.tflite",
            "file:///android_asset/drivepal-labels.txt",
            512,
            3840)
}