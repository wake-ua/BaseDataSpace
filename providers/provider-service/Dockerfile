FROM openjdk:17-ea-slim

COPY build/libs/provider-service.jar provider-service.jar
COPY resources/configuration/provider-service-docker-configuration.properties provider-service-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-service-docker-configuration.properties", "-jar", "/provider-service.jar", "--log-level=DEBUG"]
