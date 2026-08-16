#!/usr/bin/env bash
set -euo pipefail

: "${BENCHMARK_PREPARE_ONLY:=false}"
: "${JAVA_TOOL_OPTIONS:=-Xmx3g}"

if [[ "$BENCHMARK_PREPARE_ONLY" != "true" && "$BENCHMARK_PREPARE_ONLY" != "false" ]]; then
  echo "BENCHMARK_PREPARE_ONLY must be exactly 'true' or 'false'" >&2
  exit 2
fi

prepare_classes() {
  echo "Preparing shared benchmark classes and dependencies"
  rm -rf /tmp/quizzes-remove-add-benchmark-simulator
  cp -R /workspace/simulator /tmp/quizzes-remove-add-benchmark-simulator
  mvn -q -DskipTests -Dprotobuf.skip \
    -f /tmp/quizzes-remove-add-benchmark-simulator/pom.xml clean install
  (cd /verifiers && mvn -q -Dmaven.test.skip=true install)
  (cd /applications/quizzes && mvn -q -Ptest-sagas -DskipTests test-compile)
}

if [[ "$BENCHMARK_PREPARE_ONLY" == "true" ]]; then
  prepare_classes
  echo "Benchmark preparation complete"
  exit 0
fi

required=(
  PACKAGE_PATH WORKLOAD_PLAN_ID FAULT_SCENARIO_ID EXECUTION_OUTPUT_PATH
  IMPACT_OUTPUT_PATH RESULT_OUTPUT_PATH REPETITION RUNTIME_CONTEXT_ID
  SOURCE_REVISION SOURCE_TREE_STATE
)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "$name is required" >&2
    exit 2
  fi
done

for input in "$PACKAGE_PATH" /verifiers/target/classes \
  /applications/quizzes/target/classes /applications/quizzes/target/test-classes; do
  if [[ ! -e "$input" ]]; then
    echo "Prepared benchmark input does not exist: $input" >&2
    exit 2
  fi
done

mkdir -p "$(dirname "$EXECUTION_OUTPUT_PATH")" \
  "$(dirname "$IMPACT_OUTPUT_PATH")" "$(dirname "$RESULT_OUTPUT_PATH")" \
  /tmp/quizzes-remove-add-benchmark

(cd /verifiers && mvn -q dependency:build-classpath \
  -Dmdep.outputFile=/tmp/quizzes-remove-add-benchmark/verifiers-classpath.txt)
(cd /applications/quizzes && mvn -q -Ptest-sagas dependency:build-classpath \
  -Dmdep.outputFile=/tmp/quizzes-remove-add-benchmark/app-classpath.txt)

CP="/applications/quizzes/target/classes:/applications/quizzes/target/test-classes:/verifiers/target/classes:$(tr -d '\n' < /tmp/quizzes-remove-add-benchmark/app-classpath.txt):$(tr -d '\n' < /tmp/quizzes-remove-add-benchmark/verifiers-classpath.txt)"

# The generic compose service exposes SPRING_PROFILES for its own wrapper. Spring Boot
# treats that name as the removed spring.profiles property, so the benchmark runner uses
# its pinned profiles and removes the inherited wrapper variable before startup.
unset SPRING_PROFILES

java -cp "$CP" \
  pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesRemoveAddBenchmarkRunner \
  --package-path "$PACKAGE_PATH" \
  --workload-plan-id "$WORKLOAD_PLAN_ID" \
  --fault-scenario-id "$FAULT_SCENARIO_ID" \
  --execution-output-path "$EXECUTION_OUTPUT_PATH" \
  --impact-output-path "$IMPACT_OUTPUT_PATH" \
  --result-output-path "$RESULT_OUTPUT_PATH" \
  --repetition "$REPETITION" \
  --runtime-context-id "$RUNTIME_CONTEXT_ID" \
  --source-revision "$SOURCE_REVISION" \
  --source-tree-state "$SOURCE_TREE_STATE"
