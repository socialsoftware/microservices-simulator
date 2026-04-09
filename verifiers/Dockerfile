FROM maven:3.9.7-eclipse-temurin-21 AS base

ARG PROFILE=sagas,local

# 1. Build and install simulator JAR into local Maven repo (skip protobuf plugin which fails on ARM64 Docker)
COPY ./simulator/pom.xml ./simulator/pom.xml
COPY ./simulator/src ./simulator/src
RUN mvn -f ./simulator/pom.xml clean install -DskipTests -Dprotobuf.skip -DactivatedProperties=$PROFILE

# 1.5. Build and install all application JARs (generic — picks up any app under applications/)
COPY ./applications ./applications
RUN for pom in applications/*/pom.xml; do \
        mvn -f "$pom" clean install -DskipTests; \
    done && \
    mkdir -p /app-jars && \
    find applications/*/target -maxdepth 1 -name "*.jar" \
        ! -name "*-exec.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" \
        -exec cp {} /app-jars/ \;

# 2. Build verifiers
COPY ./verifiers/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./verifiers/src ./src

FROM base AS run
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
ARG PROFILE=sagas,local
ARG APP_VERSION=1.0.0-SNAPSHOT
COPY --from=run target/verifiers-*-exec.jar ./app.jar
COPY --from=base /app-jars/ /app-jars/
COPY --from=base /applications /applications
ENV SPRING_PROFILES_ACTIVE=$PROFILE
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "./app.jar"]
