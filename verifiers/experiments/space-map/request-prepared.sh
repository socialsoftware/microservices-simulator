#!/usr/bin/env bash
set -euo pipefail
: "${BUILD_OUTPUT_DIR:?}"
: "${REQUESTS_PATH:?}"
: "${RESULTS_PATH:?}"
cp_value="$BUILD_OUTPUT_DIR/source/verifiers/target/classes:$(tr -d '\n' < "$BUILD_OUTPUT_DIR/verifiers-classpath.txt")"
java -cp "$cp_value" /verifiers/experiments/space-map/RequestBatch.java "$REQUESTS_PATH" "$RESULTS_PATH"
