FROM openjdk:17-ea-slim

COPY build/libs/consumer-base.jar consumer-base.jar
COPY resources/configuration/consumer-base-docker-configuration.properties consumer-base-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=consumer-base-docker-configuration.properties", "-jar", "/consumer-base.jar", "--log-level=DEBUG"]
