FROM openjdk:11
COPY ./target/dps_server.jar /usr/local/lib/app.jar
ENTRYPOINT ["java", "-jar", "/usr/local/lib/app.jar"]
