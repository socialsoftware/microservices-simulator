# http://whitfin.io/speeding-up-maven-docker-builds/

FROM maven:3.9.7-eclipse-temurin-21 AS base

COPY ./simulator/pom.xml ./simulator/pom.xml
COPY ./simulator/src ./simulator/src
RUN mvn -f ./simulator/pom.xml clean install -DskipTests

COPY ./applications/quizzes-full/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./applications/quizzes-full/src ./src

FROM base AS test
CMD ["mvn", "-Ptest", "test"]

FROM base AS run
ARG PROFILE=sagas,stream
RUN mvn clean -P$PROFILE,kubernetes -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
ARG APP_VERSION=3.1.3-SNAPSHOT
ENV JAR_FILE=QuizzesFullSimulator-${APP_VERSION}.jar
ENV START_CLASS=pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSimulator
COPY --from=run target/QuizzesFullSimulator-*.jar ./app.jar
CMD ["sh", "-c", "java -Dloader.main=$START_CLASS -Djava.security.egd=file:/dev/./urandom -jar ./app.jar"]
