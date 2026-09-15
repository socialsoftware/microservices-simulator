#!/usr/bin/env bash
set -euo pipefail
build=/reports/empty-event-delivery/run-01/prepared-build
cp_value="$build/source/quizzes/target/classes:$build/source/quizzes/target/test-classes:$build/source/verifiers/target/classes:$(cat "$build/quizzes-classpath.txt"):$(cat "$build/verifiers-classpath.txt")"
mkdir -p /out/classes
javac -cp "$cp_value" -d /out/classes /out/diagnostic/*.java /experiment/OrdinaryExecutorControl.java
while IFS=$'\t' read -r label scenario; do
  mkdir "/out/$label"
  set +e
  java -Xmx1536m -XX:MaxMetaspaceSize=512m -Dlocal.messaging.serialize=true \
    -Dimpact.v2.fixture-now=2030-01-01T12:00:00 \
    -Dimpact.v2.qualification.witness-output="/out/$label/final-state.json" \
    -cp "/out/classes:$cp_value" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.OrdinaryExecutorControl \
    --spring-application-class pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator \
    --spring-profiles test,sagas,local --application-base quizzes --application-id quizzes \
    --maven-profile test-sagas --package-path /out/package/scenario-catalog-manifest.json \
    --fault-scenario-id "$scenario" --output-path "/out/$label/execution-report.json" \
    --impact-output-path "/out/$label/execution-report.impact.json" \
    --microservices.simulator.saga-read-exposure.enabled=true \
    --verifiers.application.enabled=false --server.port=0 > "/out/$label/runtime.log" 2>&1
  result=$?
  set -e
  echo "$result" > "/out/$label/exit-code.txt"
done < /out/selection.tsv
