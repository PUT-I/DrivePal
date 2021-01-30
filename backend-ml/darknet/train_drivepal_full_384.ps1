New-Item -ItemType Directory -Force -Path "./backup-drivepal_384"
./darknet.exe detector train "./data/drivepal_384.data" "./cfg/yolov4-tiny-drivepal_384.cfg" "./weights/yolov4-tiny.conv.29"