#!/usr/bin/env bash
set -euo pipefail

: "${BUILD_OUTPUT_DIR:=/reports/impact-v2-broader/prepared-build}"
: "${JAVA_TOOL_OPTIONS:=-Xmx1536m -XX:MaxMetaspaceSize=512m}"
: "${RUNNER_KIND:?RUNNER_KIND must be generic or historical}"
: "${PACKAGE_PATH:?PACKAGE_PATH is required}"
: "${FAULT_SCENARIO_ID:?FAULT_SCENARIO_ID is required}"
: "${EXECUTION_OUTPUT_PATH:?EXECUTION_OUTPUT_PATH is required}"
: "${IMPACT_OUTPUT_PATH:?IMPACT_OUTPUT_PATH is required}"

test "$(cat "$BUILD_OUTPUT_DIR/status")" = ready
test -d "$BUILD_OUTPUT_DIR/source/verifiers/target/classes"
test -d "$BUILD_OUTPUT_DIR/source/quizzes/target/classes"
test -d "$BUILD_OUTPUT_DIR/source/quizzes/target/test-classes"
mkdir -p "$(dirname "$EXECUTION_OUTPUT_PATH")" "$(dirname "$IMPACT_OUTPUT_PATH")"

cp_value="$BUILD_OUTPUT_DIR/source/quizzes/target/classes:$BUILD_OUTPUT_DIR/source/quizzes/target/test-classes:$BUILD_OUTPUT_DIR/source/verifiers/target/classes:$(tr -d '\n' < "$BUILD_OUTPUT_DIR/quizzes-classpath.txt"):$(tr -d '\n' < "$BUILD_OUTPUT_DIR/verifiers-classpath.txt")"
unset SPRING_PROFILES SPRING_PROFILES_ACTIVE

if [[ "$RUNNER_KIND" == generic ]]; then
  java -Dmicroservices.simulator.event-replay.enabled=true -cp "$cp_value" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
    --spring-application-class pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator \
    --spring-profiles test,sagas,local --application-base quizzes --application-id quizzes \
    --maven-profile test-sagas --package-path "$PACKAGE_PATH" \
    --fault-scenario-id "$FAULT_SCENARIO_ID" --output-path "$EXECUTION_OUTPUT_PATH" \
    --impact-output-path "$IMPACT_OUTPUT_PATH" --verifiers.application.enabled=false --server.port=0
elif [[ "$RUNNER_KIND" == historical ]]; then
  : "${WORKLOAD_PLAN_ID:?WORKLOAD_PLAN_ID is required}"
  : "${RESULT_OUTPUT_PATH:?RESULT_OUTPUT_PATH is required}"
  : "${RUNTIME_CONTEXT_ID:?RUNTIME_CONTEXT_ID is required}"
  : "${SOURCE_REVISION:?SOURCE_REVISION is required}"
  : "${SOURCE_TREE_STATE:?SOURCE_TREE_STATE is required}"
  mkdir -p "$(dirname "$RESULT_OUTPUT_PATH")"
  java -Dmicroservices.simulator.event-replay.enabled=true -cp "$cp_value" \
    pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesRemoveAddBenchmarkRunner \
    --package-path "$PACKAGE_PATH" --workload-plan-id "$WORKLOAD_PLAN_ID" \
    --fault-scenario-id "$FAULT_SCENARIO_ID" --execution-output-path "$EXECUTION_OUTPUT_PATH" \
    --impact-output-path "$IMPACT_OUTPUT_PATH" --result-output-path "$RESULT_OUTPUT_PATH" \
    --repetition 1 --runtime-context-id "$RUNTIME_CONTEXT_ID" \
    --source-revision "$SOURCE_REVISION" --source-tree-state "$SOURCE_TREE_STATE"
else
  echo "Unsupported RUNNER_KIND: $RUNNER_KIND" >&2
  exit 2
fi
