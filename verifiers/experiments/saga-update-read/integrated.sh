#!/usr/bin/env bash
set -euo pipefail
build=/reports/empty-event-delivery/run-01/prepared-build
mkdir /out/classes
cp_value="$build/source/quizzes/target/classes:$build/source/quizzes/target/test-classes:$build/source/verifiers/target/classes:$(cat "$build/quizzes-classpath.txt"):$(cat "$build/verifiers-classpath.txt"):/out/jol.jar"
java -version > /out/java-version.txt 2>&1
javac -cp "$cp_value" -d /out/classes /out/source/diagnostic/*.java /out/source/update/*.java /out/source/creation/*.java
for mode in false true; do
  for case_id in success-between fault-between fault-before fault-after; do
    java -Xmx1536m -XX:MaxMetaspaceSize=512m -Dlocal.messaging.serialize="$mode" \
      -Dexperiment.fixtureNow=2030-01-01T14:55:00 -Dexperiment.productionDiagnostic=true \
      -cp "/out/classes:$cp_value" \
      pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.updateread.SagaUpdateReadExperiment \
      "$case_id" "/out/$case_id-$mode.json" > "/out/$case_id-$mode.log" 2>&1
  done
done
for case_id in reader-only early-compensation; do
  mkdir "/out/creation-$case_id"
  java -Xmx1536m -javaagent:/out/jol.jar -cp "/out/classes:$cp_value" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureExperiment \
    "$case_id" false true "regression:$case_id" "/out/creation-$case_id" > "/out/creation-$case_id/runtime.log" 2>&1
done
mkdir /out/ordinary
java -Xmx1536m -cp "/out/classes:$cp_value" \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.OrdinaryExecutorControl \
  --spring-application-class pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator \
  --spring-profiles test,sagas,local --application-base quizzes --application-id quizzes \
  --maven-profile test-sagas --package-path /out/ordinary-control-package/scenario-catalog-manifest.json \
  --fault-scenario-id b2f2c737ada60d3e7171259f188ffcb0281feaa3364a6b2e2743bec6c6c0760a \
  --output-path /out/ordinary/execution-report.json --impact-output-path /out/ordinary/execution-report.impact.json \
  --microservices.simulator.saga-read-exposure.enabled=true --verifiers.application.enabled=false --server.port=0 \
  > /out/ordinary/runtime.log 2>&1
