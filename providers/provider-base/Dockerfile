FROM openjdk:17-ea-slim

COPY build/libs/provider-base.jar provider-base.jar
COPY resources/configuration/provider-base-docker-configuration.properties provider-base-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-base-docker-configuration.properties", "-jar", "/provider-base.jar", "--log-level=DEBUG"]
