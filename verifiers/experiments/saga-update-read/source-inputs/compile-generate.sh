#!/usr/bin/env bash
set -euo pipefail
build=/reports/empty-event-delivery/run-01/prepared-build
cp_value="$build/source/quizzes/target/classes:$build/source/quizzes/target/test-classes:$build/source/verifiers/target/classes:$(cat "$build/quizzes-classpath.txt"):$(cat "$build/verifiers-classpath.txt")"
mkdir -p /out/classes
find /out/production -name '*.java' > /out/production-sources.txt
javac -cp "$cp_value" -d /out/classes @/out/production-sources.txt /experiment/GenerateSourceUpdateReadPackage.java /experiment/OrdinaryExecutorControl.java
java -Xmx2g -cp "/out/classes:$cp_value" GenerateSourceUpdateReadPackage "$build/source" /out/package
