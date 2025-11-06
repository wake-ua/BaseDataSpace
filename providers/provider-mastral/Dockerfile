FROM openjdk:17-ea-slim

COPY build/libs/provider-mastral.jar provider-mastral.jar
COPY resources/configuration/provider-mastral-docker-configuration.properties provider-mastral-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-mastral-docker-configuration.properties", "-jar", "/provider-mastral.jar", "--log-level=DEBUG"]
