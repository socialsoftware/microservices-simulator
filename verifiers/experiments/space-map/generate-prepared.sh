#!/usr/bin/env bash
set -euo pipefail
: "${BUILD_OUTPUT_DIR:?}"
: "${GENERATION_OUTPUT_DIR:?}"
unset SPRING_PROFILES SPRING_PROFILES_ACTIVE
cp_value="$BUILD_OUTPUT_DIR/source/verifiers/target/classes:$(tr -d '\n' < "$BUILD_OUTPUT_DIR/verifiers-classpath.txt")"
java -cp "$cp_value" pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ScenarioGeneratorApplication \
  --verifiers.applications-root="$BUILD_OUTPUT_DIR/source" \
  --verifiers.application-base-dir=quizzes --verifiers.output-root="$GENERATION_OUTPUT_DIR" \
  --verifiers.scenario-catalog.enabled=true --verifiers.scenario-catalog.include-singles=true \
  --verifiers.scenario-catalog.max-saga-set-size=3 --verifiers.scenario-catalog.max-catalog-scenarios=50000 \
  --verifiers.scenario-catalog.max-input-variants-per-saga=10 \
  --verifiers.scenario-catalog.max-schedules-per-input-tuple=1 \
  --verifiers.scenario-catalog.max-event-consequences-per-workload=3 \
  --verifiers.scenario-catalog.recovery-schedule-cap=1 \
  --verifiers.scenario-catalog.schedule-strategy=SERIAL \
  --verifiers.scenario-catalog.allow-type-only-fallback=false --verifiers.dynamic-enrichment.enabled=false
