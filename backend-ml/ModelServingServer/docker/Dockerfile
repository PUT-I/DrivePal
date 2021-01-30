FROM python:3.7

WORKDIR ./app/

COPY ./AndroidTFLiteDetector/lib/src/main/assets/*.tflite ./assets/
COPY ./ModelServingServer/server/ ./server/
COPY ./ModelServingServer/requirements-docker.txt ./requirements.txt

ENV PYTHONPATH "${PYTHONPATH}:/app/server/"
RUN pip install -r requirements.txt

EXPOSE 5001
ENTRYPOINT ["python", "./server/main.py", "--models-path", "./assets"]
