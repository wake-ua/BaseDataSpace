FROM openjdk:17-ea-slim

COPY build/libs/federated-catalog.jar federated-catalog.jar
COPY resources/configuration/fc-docker-configuration.properties fc-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=fc-docker-configuration.properties", "-jar", "/federated-catalog.jar", "--log-level=DEBUG"]
