#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-/reports/impact-updates}"
SOURCE_REVISION="${SOURCE_REVISION:-unknown}"
BUILD_DIR="$(mktemp -d /tmp/impact-updates.XXXXXX)"
trap 'rm -rf "$BUILD_DIR"' EXIT

mkdir -p "$OUT_DIR" "$BUILD_DIR/simulator" "$BUILD_DIR/verifiers" \
  "$BUILD_DIR/app-base" "$BUILD_DIR/app-question-repaired" "$BUILD_DIR/app-compensation-noop" "$BUILD_DIR/harness-classes"

tar -C /workspace/simulator --exclude=target -cf - . | tar -C "$BUILD_DIR/simulator" -xf -
tar -C /verifiers --exclude=target -cf - . | tar -C "$BUILD_DIR/verifiers" -xf -
for app_dir in app-base app-question-repaired app-compensation-noop; do
  tar -C /applications/quizzes --exclude=target -cf - . | tar -C "$BUILD_DIR/$app_dir" -xf -
done

QUIZ_SERVICE_REL="src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java"
UPDATE_SAGA_REL="src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java"
PATCH_ROOT="/verifiers/experiments/impact-updates/patches"

QUIZ_SERVICE_SHA="$(sha256sum "$BUILD_DIR/app-base/$QUIZ_SERVICE_REL" | awk '{print $1}')"
UPDATE_SAGA_SHA="$(sha256sum "$BUILD_DIR/app-base/$UPDATE_SAGA_REL" | awk '{print $1}')"
QUIZ_PATCH_SHA="$(sha256sum "$PATCH_ROOT/quiz-update-register-changed.patch" | awk '{print $1}')"
COMP_PATCH_SHA="$(sha256sum "$PATCH_ROOT/update-tournament-noop-compensation.patch" | awk '{print $1}')"

REPAIR_TARGET="$BUILD_DIR/app-question-repaired/$QUIZ_SERVICE_REL"
MUTANT_TARGET="$BUILD_DIR/app-compensation-noop/$UPDATE_SAGA_REL"
QUIZ_ANCHOR='            quizQuestion.setQuestionVersion(aggregateVersion);'
COMP_DECL='            UpdateTournamentCommand updateTournamentCommand = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), originalTournamentDto, originalTournamentDto.getTopics());'
COMP_SEND='            commandGateway.send(updateTournamentCommand);'
test "$(grep -Fxc "$QUIZ_ANCHOR" "$REPAIR_TARGET")" -eq 1
test "$(grep -Fxc "$COMP_DECL" "$MUTANT_TARGET")" -eq 1
test "$(grep -Fxc "$COMP_SEND" "$MUTANT_TARGET")" -eq 1
sed -i "/quizQuestion.setQuestionVersion(aggregateVersion);/a\\            unitOfWorkService.registerChanged(newQuiz, unitOfWork);" "$REPAIR_TARGET"
sed -i "s|$COMP_DECL|            // Controlled research mutant: preserve the checkpoint but suppress its restoring write.|" "$MUTANT_TARGET"
sed -i "/^[[:space:]]*commandGateway.send(updateTournamentCommand);$/d" "$MUTANT_TARGET"
set +e
diff -u --label "a/$QUIZ_SERVICE_REL" --label "b/$QUIZ_SERVICE_REL" \
  "$BUILD_DIR/app-base/$QUIZ_SERVICE_REL" "$REPAIR_TARGET" > "$BUILD_DIR/applied-quiz-repair.patch"
QUIZ_DIFF_STATUS=$?
diff -u --label "a/$UPDATE_SAGA_REL" --label "b/$UPDATE_SAGA_REL" \
  "$BUILD_DIR/app-base/$UPDATE_SAGA_REL" "$MUTANT_TARGET" > "$BUILD_DIR/applied-compensation-mutant.patch"
COMP_DIFF_STATUS=$?
set -e
test "$QUIZ_DIFF_STATUS" -eq 1
test "$COMP_DIFF_STATUS" -eq 1
cmp -s "$BUILD_DIR/applied-quiz-repair.patch" "$PATCH_ROOT/quiz-update-register-changed.patch"
cmp -s "$BUILD_DIR/applied-compensation-mutant.patch" "$PATCH_ROOT/update-tournament-noop-compensation.patch"
REPAIRED_QUIZ_SERVICE_SHA="$(sha256sum "$REPAIR_TARGET" | awk '{print $1}')"
MUTANT_UPDATE_SAGA_SHA="$(sha256sum "$MUTANT_TARGET" | awk '{print $1}')"

mvn -q -DskipTests -Dprotobuf.skip -f "$BUILD_DIR/simulator/pom.xml" install
mvn -q -Dmaven.test.skip=true -f "$BUILD_DIR/verifiers/pom.xml" install

build_app() {
  local app_dir="$1"
  mvn -q -Ptest-sagas -DskipTests -f "$BUILD_DIR/$app_dir/pom.xml" test-compile dependency:build-classpath \
    -Dmdep.outputFile="$BUILD_DIR/$app_dir/classpath.txt"
}

build_app app-base
build_app app-question-repaired
build_app app-compensation-noop

app_cp() {
  local app_dir="$1"
  printf '%s' "$BUILD_DIR/$app_dir/target/classes:$BUILD_DIR/$app_dir/target/test-classes:$(<"$BUILD_DIR/$app_dir/classpath.txt")"
}

BASE_CP="$(app_cp app-base)"
javac -cp "$BASE_CP" -d "$BUILD_DIR/harness-classes" \
  /verifiers/experiments/impact-updates/ImpactUpdatesExperiment.java
unset SPRING_PROFILES SPRING_PROFILES_ACTIVE
FIXTURE_NOW="$(date -u +%Y-%m-%dT%H:%M:%S.000)"

run_case() {
  local case_id="$1"
  local variant="$2"
  local app_dir="$3"
  local patch_sha="$4"
  local variant_quiz_sha="$5"
  local variant_update_sha="$6"
  local cp
  cp="$(app_cp "$app_dir")"
  java \
    -Dmicroservices.simulator.event-replay.enabled=true \
    -Dexperiment.variant="$variant" \
    -Dexperiment.sourceRevision="$SOURCE_REVISION" \
    -Dexperiment.quizServiceSha256="$QUIZ_SERVICE_SHA" \
    -Dexperiment.updateTournamentSha256="$UPDATE_SAGA_SHA" \
    -Dexperiment.variantQuizServiceSha256="$variant_quiz_sha" \
    -Dexperiment.variantUpdateTournamentSha256="$variant_update_sha" \
    -Dexperiment.patchSha256="$patch_sha" \
    -Dexperiment.fixtureNow="$FIXTURE_NOW" \
    -cp "$BUILD_DIR/harness-classes:$cp" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.updates.ImpactUpdatesExperiment \
    "$case_id" "$OUT_DIR/$case_id.json" > "$OUT_DIR/$case_id.log" 2>&1
  echo "$case_id passed"
}

run_case question-baseline current-application app-base none "$QUIZ_SERVICE_SHA" "$UPDATE_SAGA_SHA"
run_case question-repaired one-line-registerChanged-control app-question-repaired "$QUIZ_PATCH_SHA" "$REPAIRED_QUIZ_SERVICE_SHA" "$UPDATE_SAGA_SHA"
run_case tournament-normal-compensation current-application app-base none "$QUIZ_SERVICE_SHA" "$UPDATE_SAGA_SHA"
run_case tournament-noop-compensation controlled-noop-compensation-mutant app-compensation-noop "$COMP_PATCH_SHA" "$QUIZ_SERVICE_SHA" "$MUTANT_UPDATE_SAGA_SHA"

{
  printf 'sourceRevision=%s\n' "$SOURCE_REVISION"
  printf 'originalQuizServiceSha256=%s\n' "$QUIZ_SERVICE_SHA"
  printf 'originalUpdateTournamentSagaSha256=%s\n' "$UPDATE_SAGA_SHA"
  printf 'quizRepairPatchSha256=%s\n' "$QUIZ_PATCH_SHA"
  printf 'compensationMutantPatchSha256=%s\n' "$COMP_PATCH_SHA"
  printf 'repairedQuizServiceSha256=%s\n' "$REPAIRED_QUIZ_SERVICE_SHA"
  printf 'mutantUpdateTournamentSagaSha256=%s\n' "$MUTANT_UPDATE_SAGA_SHA"
  printf 'fixtureNow=%s\n' "$FIXTURE_NOW"
  printf 'experimentHarnessSha256=%s\n' "$(sha256sum /verifiers/experiments/impact-updates/ImpactUpdatesExperiment.java | awk '{print $1}')"
  printf 'runnerSha256=%s\n' "$(sha256sum /verifiers/experiments/impact-updates/run.sh | awk '{print $1}')"
  sha256sum "$OUT_DIR"/question-baseline.json "$OUT_DIR"/question-repaired.json \
    "$OUT_DIR"/tournament-normal-compensation.json "$OUT_DIR"/tournament-noop-compensation.json
} > "$OUT_DIR/artifact-hashes.txt"

echo "impact-updates four-case execution passed; run validate.py on the host"
