# http://whitfin.io/speeding-up-maven-docker-builds/

FROM maven:3.9.7-eclipse-temurin-21-alpine AS base

COPY ./simulator/pom.xml ./simulator/pom.xml
COPY ./simulator/src ./simulator/src
RUN mvn -f ./simulator/pom.xml clean install -DskipTests

COPY ./applications/quizzes/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./applications/quizzes/src ./src

FROM base AS test
CMD ["mvn", "-Ptest", "test"]

FROM base AS run
ARG PROFILE=tcc,dev
RUN mvn clean -P$PROFILE -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=3.0.0-SNAPSHOT
ENV JAR_FILE=QuizzesTutor-${APP_VERSION}.jar
COPY --from=run target/QuizzesTutor-*.jar ./app.jar
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar", "./app.jar"]