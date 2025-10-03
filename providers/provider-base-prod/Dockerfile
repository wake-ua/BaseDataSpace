FROM openjdk:17-slim

COPY build/libs/provider-base-prod.jar provider-base-prod.jar
COPY resources/configuration/provider-base-prod-docker-configuration.properties provider-base-prod-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=provider-base-prod-docker-configuration.properties", "-jar", "/provider-base-prod.jar", "--log-level=DEBUG"]
