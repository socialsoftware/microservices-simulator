#!/usr/bin/env bash
set -euo pipefail
build=/reports/empty-event-delivery/run-01/prepared-build
mkdir /out/classes
cp_value="$build/source/quizzes/target/classes:$build/source/quizzes/target/test-classes:$build/source/verifiers/target/classes:$(cat "$build/quizzes-classpath.txt"):$(cat "$build/verifiers-classpath.txt")"
java -version > /out/java-version.txt 2>&1
javac -cp "$cp_value" -d /out/classes /experiment/SagaUpdateReadExperiment.java
for mode in false true; do
  for case_id in success-between fault-between fault-before fault-after; do
    java -Xmx1536m -XX:MaxMetaspaceSize=512m -Dlocal.messaging.serialize="$mode" \
      -Dexperiment.fixtureNow=2030-01-01T14:55:00 \
      -cp "/out/classes:$cp_value" \
      pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.updateread.SagaUpdateReadExperiment \
      "$case_id" "/out/$case_id-$mode.json" > "/out/$case_id-$mode.log" 2>&1
  done
done
