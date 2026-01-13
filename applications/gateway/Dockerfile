# Gateway Dockerfile
FROM maven:3.9.7-eclipse-temurin-21-alpine AS base

COPY ./simulator/pom.xml ./simulator/pom.xml
COPY ./simulator/src ./simulator/src
RUN mvn -f ./simulator/pom.xml clean install -DskipTests

COPY ./applications/gateway/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./applications/gateway/src ./src

FROM base AS run
RUN mvn clean -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=0.0.1-SNAPSHOT
ENV JAR_FILE=gateway-${APP_VERSION}.jar
COPY --from=run target/gateway-*.jar ./app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar", "./app.jar"]
