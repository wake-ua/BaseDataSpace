FROM openjdk:17-slim

COPY build/libs/provider-ebird.jar provider-ebird.jar
COPY resources/configuration/provider-ebird-docker-configuration.properties provider-ebird-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-ebird-docker-configuration.properties", "-jar", "/provider-ebird.jar", "--log-level=DEBUG"]
