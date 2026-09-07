#!/usr/bin/env bash
set -euo pipefail
: "${BUILD_OUTPUT_DIR:?Use a new directory under /reports}"

bash /verifiers/experiments/impact-v2-broader/prepare-build.sh

# Reuse the existing qualification provider solely for the known Question->Quiz
# positive. It is copied unchanged to the private test tree; application production
# remains the source snapshot prepared above.
provider=pt/ulisboa/tecnico/socialsoftware/quizzes/executor/QuizzesImpactV2PrerequisiteProvider.java
mkdir -p "$BUILD_OUTPUT_DIR/source/quizzes/src/test/java/$(dirname "$provider")"
cp /verifiers/experiments/impact-v2/fixtures/QuizzesImpactV2PrerequisiteProvider.java \
  "$BUILD_OUTPUT_DIR/source/quizzes/src/test/java/$provider"
qualification_cp="$BUILD_OUTPUT_DIR/source/quizzes/target/classes:$BUILD_OUTPUT_DIR/source/quizzes/target/test-classes:$BUILD_OUTPUT_DIR/source/verifiers/target/classes:$(cat "$BUILD_OUTPUT_DIR/quizzes-classpath.txt"):$(cat "$BUILD_OUTPUT_DIR/verifiers-classpath.txt")"
javac -cp "$qualification_cp" -d "$BUILD_OUTPUT_DIR/source/quizzes/target/test-classes" \
  "$BUILD_OUTPUT_DIR/source/quizzes/src/test/java/$provider"
