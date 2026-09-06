#!/usr/bin/env bash
set -euo pipefail

: "${BUILD_OUTPUT_DIR:=/reports/impact-v2-broader/prepared-build}"
: "${JAVA_TOOL_OPTIONS:=-Xmx1536m -XX:MaxMetaspaceSize=512m}"

mkdir -p "$BUILD_OUTPUT_DIR"
if find "$BUILD_OUTPUT_DIR" -mindepth 1 -print -quit | grep -q .; then
  echo "Refusing non-empty prepared build directory: $BUILD_OUTPUT_DIR" >&2
  exit 2
fi
mkdir -p "$BUILD_OUTPUT_DIR/source/simulator" "$BUILD_OUTPUT_DIR/source/verifiers" \
  "$BUILD_OUTPUT_DIR/source/quizzes"
tar -C /workspace/simulator --exclude=target --exclude=.git --exclude=logs \
  --exclude=__pycache__ --exclude='*.pyc' -cf - . | \
  tar -C "$BUILD_OUTPUT_DIR/source/simulator" -xf -
tar -C /verifiers --exclude=target --exclude=.git --exclude=logs --exclude=experiments \
  --exclude=__pycache__ --exclude='*.pyc' -cf - . | \
  tar -C "$BUILD_OUTPUT_DIR/source/verifiers" -xf -
tar -C /applications/quizzes --exclude=target --exclude=.git --exclude=logs \
  --exclude=__pycache__ --exclude='*.pyc' -cf - . | \
  tar -C "$BUILD_OUTPUT_DIR/source/quizzes" -xf -
mvn -q -DskipTests -Dprotobuf.skip -f "$BUILD_OUTPUT_DIR/source/simulator/pom.xml" clean install
(cd "$BUILD_OUTPUT_DIR/source/verifiers" && mvn -q -Dmaven.test.skip=true install && mvn -q dependency:build-classpath \
  -Dmdep.outputFile="$BUILD_OUTPUT_DIR/verifiers-classpath.txt")
(cd "$BUILD_OUTPUT_DIR/source/quizzes" && mvn -q -Ptest-sagas -DskipTests test-compile dependency:build-classpath \
  -Dmdep.outputFile="$BUILD_OUTPUT_DIR/quizzes-classpath.txt")
printf '%s\n' ready > "$BUILD_OUTPUT_DIR/status"
