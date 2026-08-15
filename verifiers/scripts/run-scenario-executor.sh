#!/usr/bin/env bash
set -euo pipefail

: "${APPLICATION_BASE_DIR:=quizzes}"
: "${SPRING_APPLICATION_CLASS:=pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator}"
: "${MAVEN_PROFILE:=test-sagas}"
: "${SPRING_PROFILES:=test,sagas,local}"
: "${PREFLIGHT:=false}"
: "${DRY_RUN:=false}"
: "${SERVER_PORT:=0}"

for boolean_name in PREFLIGHT DRY_RUN; do
  boolean_value="${!boolean_name}"
  if [[ "$boolean_value" != "true" && "$boolean_value" != "false" ]]; then
    echo "$boolean_name must be exactly 'true' or 'false'" >&2
    exit 2
  fi
done
if [[ "$PREFLIGHT" == "true" && "$DRY_RUN" == "true" ]]; then
  echo "PREFLIGHT=true cannot be combined with DRY_RUN=true" >&2
  exit 2
fi
if [[ "$PREFLIGHT" == "true" && -n "${FAULT_SCENARIO_ID:-}" ]]; then
  echo "PREFLIGHT=true cannot be combined with FAULT_SCENARIO_ID" >&2
  exit 2
fi
if [[ "$PREFLIGHT" == "true" && -n "${IMPACT_OUTPUT_PATH:-}" ]]; then
  echo "PREFLIGHT=true cannot be combined with IMPACT_OUTPUT_PATH" >&2
  exit 2
fi

if [[ "$PREFLIGHT" == "true" ]]; then
  : "${OUTPUT_PATH:=/reports/scenario-executor/setup-preflight-report.json}"
else
  : "${OUTPUT_PATH:=/reports/scenario-executor/execution-report.json}"
fi

if [[ -z "${PACKAGE_PATH:-}" ]]; then
  echo "PACKAGE_PATH is required, e.g. /reports/<run>/scenario-catalog-manifest.json" >&2
  exit 2
fi

if [[ ! -f "$PACKAGE_PATH" ]]; then
  echo "PACKAGE_PATH does not exist in the container: $PACKAGE_PATH" >&2
  exit 2
fi

if [[ "$PREFLIGHT" != "true" && -z "${FAULT_SCENARIO_ID:-}" ]]; then
  echo "FAULT_SCENARIO_ID is required and must identify one persisted FaultScenario" >&2
  exit 2
fi

APP_DIR="/applications/${APPLICATION_BASE_DIR}"
if [[ ! -d "$APP_DIR" ]]; then
  echo "APPLICATION_BASE_DIR does not resolve to a mounted app directory: $APP_DIR" >&2
  exit 2
fi

mkdir -p "$(dirname "$OUTPUT_PATH")" /tmp/scenario-executor
if [[ -n "${IMPACT_OUTPUT_PATH:-}" ]]; then
  mkdir -p "$(dirname "$IMPACT_OUTPUT_PATH")"
fi

echo "Installing simulator dependency into Maven cache"
rm -rf /tmp/scenario-executor/simulator
cp -R /workspace/simulator /tmp/scenario-executor/simulator
mvn -q -DskipTests -Dprotobuf.skip -f /tmp/scenario-executor/simulator/pom.xml clean install

echo "Building verifier executor classes"
cd /verifiers
mvn -q -Dmaven.test.skip=true install
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/scenario-executor/verifiers-classpath.txt

echo "Preparing target application: ${APPLICATION_BASE_DIR}"
cd "$APP_DIR"
mvn -q -P"${MAVEN_PROFILE}" test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/scenario-executor/app-classpath.txt

CP="${APP_DIR}/target/classes:${APP_DIR}/target/test-classes:/verifiers/target/classes:$(tr -d '\n' < /tmp/scenario-executor/app-classpath.txt):$(tr -d '\n' < /tmp/scenario-executor/verifiers-classpath.txt)"

echo "Running scenario executor"
echo "  application: ${APPLICATION_BASE_DIR}"
echo "  package: ${PACKAGE_PATH}"
echo "  mode: $([[ "$PREFLIGHT" == "true" ]] && echo setup-preflight || echo execution)"
if [[ "$PREFLIGHT" != "true" ]]; then
  echo "  FaultScenario id: ${FAULT_SCENARIO_ID}"
fi
echo "  output: ${OUTPUT_PATH}"
if [[ -n "${IMPACT_OUTPUT_PATH:-}" ]]; then
  echo "  impact output: ${IMPACT_OUTPUT_PATH}"
fi

SPRING_PROFILES_VALUE="$SPRING_PROFILES"
unset SPRING_PROFILES

EXECUTOR_ARGS=(
  --spring-application-class "$SPRING_APPLICATION_CLASS"
  --spring-profiles "$SPRING_PROFILES_VALUE"
  --application-base "$APPLICATION_BASE_DIR"
  --application-id "$APPLICATION_BASE_DIR"
  --maven-profile "$MAVEN_PROFILE"
  --package-path "$PACKAGE_PATH"
  --output-path "$OUTPUT_PATH"
)
if [[ "$PREFLIGHT" == "true" ]]; then
  EXECUTOR_ARGS+=(--preflight)
else
  EXECUTOR_ARGS+=(--fault-scenario-id "$FAULT_SCENARIO_ID")
  if [[ "$DRY_RUN" == "true" ]]; then
    EXECUTOR_ARGS+=(--dry-run)
  fi
  if [[ -n "${IMPACT_OUTPUT_PATH:-}" ]]; then
    EXECUTOR_ARGS+=(--impact-output-path "$IMPACT_OUTPUT_PATH")
  fi
fi

java -Dmicroservices.simulator.event-replay.enabled=true -cp "$CP" \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
  "${EXECUTOR_ARGS[@]}" \
  --verifiers.application.enabled=false \
  --server.port="$SERVER_PORT"

echo "Scenario executor report: $OUTPUT_PATH"
