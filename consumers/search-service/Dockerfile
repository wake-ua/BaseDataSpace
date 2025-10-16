FROM openjdk:17-slim

COPY build/libs/search-service.jar search-service.jar
COPY resources/configuration/search-service-docker-configuration.properties search-service-docker-configuration.properties

ENTRYPOINT ["java", "-Dedc.fs.config=search-service-docker-configuration.properties", "-jar", "/search-service.jar", "--log-level=DEBUG"]
