#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-/reports/impact-behavior-probes}"
BUILD_DIR="$(mktemp -d /tmp/impact-behavior-probes.XXXXXX)"
trap 'rm -rf "$BUILD_DIR"' EXIT
mkdir -p "$OUT_DIR" "$BUILD_DIR/simulator" "$BUILD_DIR/verifiers" "$BUILD_DIR/classes"

tar -C /workspace/simulator --exclude=target -cf - . | tar -C "$BUILD_DIR/simulator" -xf -
tar -C /verifiers --exclude=target -cf - . | tar -C "$BUILD_DIR/verifiers" -xf -
mvn -q -DskipTests -Dprotobuf.skip -f "$BUILD_DIR/simulator/pom.xml" install
mvn -q -Dmaven.test.skip=true -f "$BUILD_DIR/verifiers/pom.xml" package dependency:build-classpath \
  -Dmdep.outputFile="$BUILD_DIR/verifier-cp.txt"
cd /applications/quizzes
mvn -q -Ptest-sagas test-compile dependency:build-classpath -Dmdep.outputFile="$BUILD_DIR/app-cp.txt"

PROBE_CP="$BUILD_DIR/classes:$BUILD_DIR/verifiers/target/classes:/applications/quizzes/target/classes:/applications/quizzes/target/test-classes:$(<"$BUILD_DIR/app-cp.txt"):$(<"$BUILD_DIR/verifier-cp.txt")"
javac -cp "$PROBE_CP" -d "$BUILD_DIR/classes" /verifiers/experiments/impact-three-cases/ImpactExperiment.java
unset SPRING_PROFILES

for probe_id in \
  tournament-read-affected \
  tournament-read-healthy \
  tournament-quiz-read-affected \
  tournament-quiz-read-healthy \
  answer-question-affected \
  answer-question-healthy
do
  java -Dmicroservices.simulator.event-replay.enabled=true -cp "$PROBE_CP" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.ImpactExperiment \
    --behavior-probe "$probe_id" "$OUT_DIR/$probe_id.json" > "$OUT_DIR/$probe_id.log" 2>&1
  echo "$probe_id passed"
done
