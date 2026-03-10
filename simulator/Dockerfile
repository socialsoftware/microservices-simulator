# Simulator Dockerfile for version-service
FROM maven:3.9.7-eclipse-temurin-21 AS base

COPY ./simulator/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./simulator/src ./src

FROM base AS run
ARG PROFILE=version-service,stream
RUN mvn clean package -P$PROFILE,kubernetes -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
ARG APP_VERSION=3.1.2-SNAPSHOT
ENV JAR_FILE=MicroservicesSimulator-${APP_VERSION}-exec.jar
COPY --from=run target/MicroservicesSimulator-*-exec.jar ./app.jar
ENV START_CLASS=pt.ulisboa.tecnico.socialsoftware.ms.gateway.GatewayApplication
CMD ["sh", "-c", "java -Dloader.main=$START_CLASS -Djava.security.egd=file:/dev/./urandom -jar ./app.jar"]
