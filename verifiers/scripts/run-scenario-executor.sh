#!/usr/bin/env bash
set -euo pipefail

: "${APPLICATION_BASE_DIR:=quizzes}"
: "${SPRING_APPLICATION_CLASS:=pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator}"
: "${MAVEN_PROFILE:=test-sagas}"
: "${SPRING_PROFILES:=test,sagas,local}"
: "${OUTPUT_PATH:=/reports/scenario-executor/execution-report.json}"
: "${SERVER_PORT:=0}"

if [[ -z "${CATALOG_PATH:-}" ]]; then
  echo "CATALOG_PATH is required, e.g. /reports/<run>/scenario-catalog.jsonl" >&2
  exit 2
fi

if [[ ! -f "$CATALOG_PATH" ]]; then
  echo "CATALOG_PATH does not exist in the container: $CATALOG_PATH" >&2
  exit 2
fi

APP_DIR="/applications/${APPLICATION_BASE_DIR}"
if [[ ! -d "$APP_DIR" ]]; then
  echo "APPLICATION_BASE_DIR does not resolve to a mounted app directory: $APP_DIR" >&2
  exit 2
fi

mkdir -p "$(dirname "$OUTPUT_PATH")" /tmp/scenario-executor

echo "Installing simulator dependency into Maven cache"
mvn -q -DskipTests -Dprotobuf.skip -f /workspace/simulator/pom.xml clean install

echo "Building verifier executor classes"
cd /verifiers
mvn -q -DskipTests package
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/scenario-executor/verifiers-classpath.txt

echo "Preparing target application: ${APPLICATION_BASE_DIR}"
cd "$APP_DIR"
mvn -q -P"${MAVEN_PROFILE}" test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/scenario-executor/app-classpath.txt

CP="${APP_DIR}/target/classes:${APP_DIR}/target/test-classes:/verifiers/target/classes:$(tr -d '\n' < /tmp/scenario-executor/app-classpath.txt):$(tr -d '\n' < /tmp/scenario-executor/verifiers-classpath.txt)"

SCENARIO_ARGS=()
if [[ -n "${SCENARIO_ID:-}" ]]; then
  SCENARIO_ARGS+=(--scenario-id "$SCENARIO_ID")
fi

echo "Running scenario executor"
echo "  application: ${APPLICATION_BASE_DIR}"
echo "  catalog: ${CATALOG_PATH}"
echo "  output: ${OUTPUT_PATH}"
if [[ -n "${SCENARIO_ID:-}" ]]; then
  echo "  scenario id: ${SCENARIO_ID}"
else
  echo "  scenario id: <auto-select>"
fi

java -cp "$CP" pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
  --spring-application-class "$SPRING_APPLICATION_CLASS" \
  --spring-profiles "$SPRING_PROFILES" \
  --catalog-path "$CATALOG_PATH" \
  --output-path "$OUTPUT_PATH" \
  "${SCENARIO_ARGS[@]}" \
  --verifiers.application.enabled=false \
  --server.port="$SERVER_PORT"

echo "Scenario executor report: $OUTPUT_PATH"
