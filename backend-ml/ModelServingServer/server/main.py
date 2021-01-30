import base64
import time
from typing import Tuple

import numpy as np
import orjson
import tensorflow as tf
from flasgger import Swagger
from flask import Flask, request

from DetectionModel import DetectionModel
from Log import Log

MODELS_PATH: str = "../../AndroidTFLiteDetector/lib/src/main/assets"

app = Flask(__name__)
swagger = Swagger(app)


def parse_arguments() -> None:
    global MODELS_PATH
    import argparse

    parser = argparse.ArgumentParser()

    # Required argument
    parser.add_argument("-p", "--models-path", type=str, required=False, help="Path to tflite files")

    args = parser.parse_args()

    if args.models_path:
        MODELS_PATH = args.models_path


parse_arguments()


def string_to_image_(image_str: str, shape: tuple) -> np.ndarray:
    image_bytes = base64.b64decode(image_str)

    result = np.frombuffer(image_bytes, dtype=np.uint8)
    result = np.reshape(result, shape)

    return result


class DetectionRequest:
    def __init__(self, json_data: dict):
        self.detection_model = DetectionModel[json_data["detectionModel"]]
        self.iou_threshold: float = json_data["iouThreshold"]
        self.score_threshold: float = json_data["scoreThreshold"]
        self.image_str = json_data["image"]


class Detector:
    def __init__(self, detection_model: DetectionModel):
        global MODELS_PATH

        # Load the TFLite model and allocate tensors.
        self.interpreter_ = tf.lite.Interpreter(model_path=f"{MODELS_PATH}/{detection_model.value}")
        self.interpreter_.allocate_tensors()

        # Get input and output tensors.
        self.input_details = self.interpreter_.get_input_details()
        self.output_details = self.interpreter_.get_output_details()

        # Test the model on random input data.
        self.input_shape: tuple = self.input_details[0]['shape']

    def recognize_image(self, image_str: str, score_threshold: float, iou_threshold: float) -> Tuple[list, list]:
        time_start: float = time.time()

        image = string_to_image_(image_str, self.input_shape)
        input_data = np.reshape(image.astype(np.float32) / 255.0, self.input_details[0]['shape'])
        Log.i(f"Image load time : {round(time.time() - time_start, 3)}s")

        time_start: float = time.time()
        self.interpreter_.set_tensor(self.input_details[0]['index'], input_data)
        self.interpreter_.invoke()
        Log.i(f"Inference time : {round(time.time() - time_start, 3)}s")

        time_start: float = time.time()
        boxes: np.ndarray = self.interpreter_.get_tensor(self.output_details[0]['index'])[0]
        scores: np.ndarray = self.interpreter_.get_tensor(self.output_details[1]['index'])[0]

        scores_flat: np.ndarray = np.zeros((scores.shape[0],), dtype=np.float32)
        value: np.ndarray
        for index, value in enumerate(scores):
            scores_flat[index] = value.max(initial=0.0, axis=0)

        filtered_indices = tf.image.non_max_suppression(
            boxes=boxes,
            scores=scores_flat,
            max_output_size=self.output_details[0]['shape'][1],
            iou_threshold=iou_threshold,
            score_threshold=score_threshold,
        )

        scores_filtered = []
        boxes_filtered = []

        for index_ in filtered_indices:
            scores_filtered.append(scores[index_].tolist())
            boxes_filtered.append(boxes[index_].tolist())
        Log.i(f"Result processing time : {round(time.time() - time_start, 3)}s")

        return boxes_filtered, scores_filtered


_detectors: dict = {}
for detection_model_ in DetectionModel:
    Log.i(f"Model setup : {detection_model_.name}")
    _detectors[detection_model_] = Detector(detection_model_)


@app.route('/test')
def test():
    """ file: apidocs/test.yml """

    Log.i("Received test request")
    return "ok"


# self.detection_model = DetectionModel[json_data["detectionModel"]]
# self.iou_threshold: float = json_data["iouThreshold"]
# self.score_threshold: float = json_data["scoreThreshold"]
# self.image_str = json_data["image"]

@app.route('/detect', methods=["POST"])
def detect():
    """ file: apidocs/detect.yml """

    global _detectors

    Log.i("Received detection request")

    time_start: float = time.time()

    try:
        detection_request = DetectionRequest(orjson.loads(request.data))
        Log.i(f"Parsing time : {round(time.time() - time_start, 3)}s")

        detector = _detectors[detection_request.detection_model]
    except KeyError:
        return "Detection model not found.", 404

    bboxes, scores = detector.recognize_image(detection_request.image_str,
                                              detection_request.score_threshold,
                                              detection_request.iou_threshold)

    result = {
        "scores": scores,
        "bboxes": bboxes
    }
    Log.i(f"Took : {round(time.time() - time_start, 3)}s")
    Log.i(result)

    # Uncomment for debugging (requires OpenCV)
    # import cv2 as cv
    # image = string_to_image_(detection_request.image_str, shape=detector.input_shape[1:])
    # image_temp = cv.cvtColor(image, cv.COLOR_RGB2BGR)
    # cv.imshow("test", image_temp)
    # cv.waitKey()
    # cv.destroyAllWindows()

    return result


if __name__ == '__main__':
    from cheroot.wsgi import Server

    address = '0.0.0.0'
    port = 5001

    Log.i(f"Starting server listening at {address}:{port}")

    server = Server((address, port), app)

    try:
        server.start()
    except KeyboardInterrupt:
        server.stop()
