# http://whitfin.io/speeding-up-maven-docker-builds/

FROM maven:3.9.7-eclipse-temurin-21-alpine as base

COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./src ./src

FROM base as test
CMD ["mvn", "-Ptest", "test"]

FROM base as run
ARG PROFILE=tcc,dev
RUN mvn clean -P$PROFILE -Dmaven.test.skip=true package

FROM openjdk:21-jdk-slim
COPY --from=run target/MicroservicesSimulator-2.0.0-SNAPSHOT.jar ./
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar", "./MicroservicesSimulator-2.0.0-SNAPSHOT.jar"]

