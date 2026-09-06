#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-/reports/impact-three-cases}"
BUILD_DIR="$(mktemp -d /tmp/impact-three-cases.XXXXXX)"
trap 'rm -rf "$BUILD_DIR"' EXIT
mkdir -p "$OUT_DIR/run-1" "$OUT_DIR/run-2" "$BUILD_DIR/simulator" "$BUILD_DIR/verifiers" "$BUILD_DIR/classes"

tar -C /workspace/simulator --exclude=target -cf - . | tar -C "$BUILD_DIR/simulator" -xf -
tar -C /verifiers --exclude=target -cf - . | tar -C "$BUILD_DIR/verifiers" -xf -
mvn -q -DskipTests -Dprotobuf.skip -f "$BUILD_DIR/simulator/pom.xml" install
mvn -q -Dmaven.test.skip=true -f "$BUILD_DIR/verifiers/pom.xml" package dependency:build-classpath -Dmdep.outputFile="$BUILD_DIR/verifier-cp.txt"
cd /applications/quizzes
mvn -q -Ptest-sagas test-compile dependency:build-classpath -Dmdep.outputFile="$BUILD_DIR/app-cp.txt"

PROBE_CP="$BUILD_DIR/classes:$BUILD_DIR/verifiers/target/classes:/applications/quizzes/target/classes:/applications/quizzes/target/test-classes:$(<"$BUILD_DIR/app-cp.txt"):$(<"$BUILD_DIR/verifier-cp.txt")"
javac -cp "$PROBE_CP" -d "$BUILD_DIR/classes" /verifiers/experiments/impact-three-cases/ImpactExperiment.java
unset SPRING_PROFILES

run_case() {
  local case_id="$1"
  local run_id="$2"
  java -Dmicroservices.simulator.event-replay.enabled=true -cp "$PROBE_CP" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.ImpactExperiment \
    "$case_id" "$OUT_DIR/$run_id/$case_id.json" > "$OUT_DIR/$run_id/$case_id.log" 2>&1
  echo "$run_id/$case_id passed"
}

run_case create-early-compensation run-1
run_case create-split-start run-1
run_case remove-fault-recovery-alone run-1
run_case create-success-overlap-control run-1
run_case create-split-start run-2
run_case create-success-overlap-control run-2
