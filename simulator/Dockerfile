# Simulator Dockerfile for version-service
FROM maven:3.9.7-eclipse-temurin-21-alpine AS base

COPY ./simulator/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./simulator/src ./src

FROM base AS run
ARG PROFILE=version-service,stream
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=3.0.0-SNAPSHOT
ENV JAR_FILE=MicroservicesSimulator-${APP_VERSION}-exec.jar
COPY --from=run target/MicroservicesSimulator-*-exec.jar ./app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar", "./app.jar"]
