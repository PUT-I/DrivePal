# DrivePal - machine learning
This directory contains project files related to machine learning part of DrivePal.

# Darknet and YoloV4 Tiny
DrivePal uses YoloV4 Tiny trained with Darknet framework.

## Installation
Before compiling Darknet install:

* [CUDA](https://developer.nvidia.com/cuda-downloads?target_os=Windows&target_arch=x86_64) + [cuDNN](https://developer.nvidia.com/rdp/cudnn-archive) (NVIDIA only)
* [OpenCV](https://opencv.org/releases/) - set **OPENCV_DIR** environmental variable to **{OPENCV_PATH_HERE}\build\x64\vc15\lib**
* [CMake](https://cmake.org/)
* [Microsoft Visual Studio](https://visualstudio.microsoft.com/pl/thank-you-downloading-visual-studio/?sku=Community&rel=16)

Darknet repository and installation guide can be found [here](https://github.com/AlexeyAB/darknet#how-to-compile-on-windows-using-cmake).

**!HINT - if build.ps1 is considered not signed by powershell run:**
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

## Data conversion
Some description here

### COCO
[Convert2Yolo](https://github.com/ssaru/convert2Yolo) project was used to convert COCO dataset to YOLO format.
COCO dataset can be downloaded from [here](https://cocodataset.org/#download). 
Download files **2017 Train images**, **2017 Val images** and **2017 Train/Val annotations**. 

Use validation images as test images.

To convert model run **example.py** script with parameters:
```powershell
--dataset COCO
--dataset_type train # or test
--img_path "COCO_DATASET_PATH\train"
--label "COCO_DATASET_PATH\annotations\annotations/panoptic_train2017.json"
--convert_output_path "COCO_DATASET_PATH\train"
--img_type jpg
--manifest_path "./example/coco" # output path
--cls_list_file "./coco.names" # list of used classes
```

## Training and demoing
Before training you must prepare some files:

* pre-trained weights - can be downloaded from [here](https://github.com/AlexeyAB/darknet/releases/download/darknet_yolo_v4_pre/yolov4-tiny.conv.29)
* .data, .names and .cfg file - use example files as baseline

### Train
To train YoloV4 network run command:
```powershell
./darknet.exe detector train "PATH_TO_DATA" "PATH_TO_CFG" "PATH_TO_CONV"
```

Command with example values:
```powershell
./darknet.exe detector train "./data/drivepal.data" "./cfg/yolov4-tiny-drivepal.cfg" "./yolov4-tiny.conv.29"
```

### Demo
To demo trained model run command:
```powershell
./darknet.exe detector demo ` 
"PATH_TO_DATA" `
"PATH_TO_CFG" `
"PATH_TO_WEIGHTS" `
-ext_output "INPUT_FILENAME" `
-out_filename "OUTPUT_FILENAME"
```

Command with example values:
```powershell
./darknet.exe detector demo ` 
"./data/obj.data" `
"./cfg/yolov4-tiny-obj.cfg" `
"./backup/yolov4-tiny-obj.weights" `
-ext_output "./test.mp4" `
-out_filename "./test-result.mp4"
```

# Android
## AndroidTFLiteDetector
AndroidTFLiteDetector is a library which provides access to ML models used by drivepal and some detections visualization helpers.

Library exposes **DetectorFactory** and **Detector** interface which can be used to create models and use them respectively.
Available models are described in **DetectionModel** enum.

## AndroidTFLiteDetectorExample
AndroidTFLiteDetectorExample is an example app which uses AndroidTFLiteDetector library.
It consists of single camera activity.

Example is based on CameraX (androidx.camera) to make camera usage as concise as it can be.