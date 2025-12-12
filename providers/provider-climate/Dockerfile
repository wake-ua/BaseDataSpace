FROM openjdk:17-ea-slim

COPY build/libs/provider-climate.jar provider-climate.jar
COPY resources/configuration/provider-climate-docker-configuration.properties provider-climate-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-climate-docker-configuration.properties", "-jar", "/provider-climate.jar", "--log-level=DEBUG"]
