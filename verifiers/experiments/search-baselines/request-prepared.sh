#!/usr/bin/env bash
set -euo pipefail
: "${BUILD_OUTPUT_DIR:?}"
: "${PACKAGE_PATH:?}"
: "${WORKLOAD_PLAN_ID:?}"
: "${FAULT_VECTOR:?}"
cp_value="$BUILD_OUTPUT_DIR/source/verifiers/target/classes:$(tr -d '\n' < "$BUILD_OUTPUT_DIR/verifiers-classpath.txt")"
java -cp "$cp_value" pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.FaultScenarioRequestCli \
  --manifest-path "$PACKAGE_PATH" --workload-plan-id "$WORKLOAD_PLAN_ID" \
  --fault-vector "$FAULT_VECTOR" --recovery-schedule-cap 20
