FROM openjdk:17-ea-slim

COPY build/libs/climate-service.jar climate-service.jar
COPY resources/configuration/climate-service-docker-configuration.properties climate-service-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=climate-service-docker-configuration.properties", "-jar", "/climate-service.jar", "--log-level=DEBUG"]
