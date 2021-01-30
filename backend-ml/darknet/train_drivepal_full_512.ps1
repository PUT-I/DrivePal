New-Item -ItemType Directory -Force -Path "./backup-drivepal_512"
./darknet.exe detector train "./data/drivepal_512.data" "./cfg/yolov4-tiny-drivepal_512.cfg" "./weights/yolov4-tiny.conv.29"