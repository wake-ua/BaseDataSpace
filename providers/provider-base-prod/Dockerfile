FROM openjdk:17-slim

COPY build/libs/provider-base.jar provider-base.jar
COPY resources/configuration/provider-base-docker-configuration.properties provider-base-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-base-prod-docker-configuration.properties", "-jar", "/provider-base-prod.jar", "--log-level=DEBUG"]
