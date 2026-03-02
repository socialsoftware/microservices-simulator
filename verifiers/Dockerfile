FROM maven:3.9.7-eclipse-temurin-21-alpine AS base
RUN apk add --no-cache gcompat

# 1. Build and install simulator JAR into local Maven repo
COPY ./simulator/pom.xml ./simulator/pom.xml
COPY ./simulator/src ./simulator/src
RUN mvn -f ./simulator/pom.xml clean install -DskipTests

# 2. Build verifiers
COPY ./verifiers/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./verifiers/src ./src

FROM base AS run
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=1.0.0-SNAPSHOT
COPY --from=run target/FaultAnalysisVerifier-*-exec.jar ./app.jar
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "./app.jar"]
