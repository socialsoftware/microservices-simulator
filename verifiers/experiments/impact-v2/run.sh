#!/usr/bin/env bash
set -euo pipefail

command -v python3 >/dev/null || {
  echo "python3 is required for fixture selection, metrics, and strict validation" >&2
  exit 127
}

OUT_DIR="${1:-/reports/impact-v2}"
: "${SOURCE_REVISION:?Set SOURCE_REVISION to the immutable source revision under qualification}"
FIXTURE_NOW="${FIXTURE_NOW:-}"
IMPACT_V2_SIDECAR_SUFFIX="${IMPACT_V2_SIDECAR_SUFFIX:-.impact-v2.json}"
IMPACT_V2_DISABLED_JAVA_ARG="${IMPACT_V2_DISABLED_JAVA_ARG:--Dmicroservices.simulator.impact.enabled=false}"
BUILD_DIR="$(mktemp -d /tmp/impact-v2-qualification.XXXXXX)"
trap 'rm -rf "$BUILD_DIR"' EXIT

EXPERIMENT_ROOT="/verifiers/experiments/impact-v2"
PATCH_ROOT="$EXPERIMENT_ROOT/patches"
PROVIDER_SOURCE="$EXPERIMENT_ROOT/fixtures/QuizzesImpactV2PrerequisiteProvider.java"
PACKAGE_DIR="$OUT_DIR/package"
if [[ -d "$OUT_DIR" && -n "$(find "$OUT_DIR" -mindepth 1 -print -quit)" ]]; then
  echo "Refusing to mix qualification attempts in non-empty output directory: $OUT_DIR" >&2
  exit 2
fi
mkdir -p "$OUT_DIR" "$PACKAGE_DIR" "$BUILD_DIR/simulator" "$BUILD_DIR/verifiers" \
  "$BUILD_DIR/app-base" "$BUILD_DIR/app-question-repaired" "$BUILD_DIR/app-compensation-noop"

tar -C /workspace/simulator --exclude=target -cf - . | tar -C "$BUILD_DIR/simulator" -xf -
tar -C /verifiers --exclude=target -cf - . | tar -C "$BUILD_DIR/verifiers" -xf -
for app_dir in app-base app-question-repaired app-compensation-noop; do
  tar -C /applications/quizzes --exclude=target -cf - . | tar -C "$BUILD_DIR/$app_dir" -xf -
  provider_target="$BUILD_DIR/$app_dir/src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/executor/QuizzesImpactV2PrerequisiteProvider.java"
  mkdir -p "$(dirname "$provider_target")"
  cp "$PROVIDER_SOURCE" "$provider_target"
done

QUIZ_SERVICE_REL="src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java"
UPDATE_SAGA_REL="src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java"
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

export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1536m -XX:MaxMetaspaceSize=512m}"
mvn -q -DskipTests -f "$BUILD_DIR/simulator/pom.xml" install
mvn -q -Dmaven.test.skip=true -f "$BUILD_DIR/verifiers/pom.xml" install
mvn -q -f "$BUILD_DIR/verifiers/pom.xml" dependency:build-classpath \
  -Dmdep.outputFile="$BUILD_DIR/verifiers-classpath.txt"
VERIFIER_CP="$BUILD_DIR/verifiers/target/classes:$(tr -d '\n' < "$BUILD_DIR/verifiers-classpath.txt")"
java -cp "$VERIFIER_CP" groovy.ui.GroovyMain \
  "$EXPERIMENT_ROOT/generate-fixture.groovy" "$PACKAGE_DIR"

build_app() {
  local app_dir="$1"
  mvn -q -Ptest-sagas -DskipTests -f "$BUILD_DIR/$app_dir/pom.xml" test-compile dependency:build-classpath \
    -Dmdep.outputFile="$BUILD_DIR/$app_dir/classpath.txt"
}

build_app app-base
build_app app-question-repaired
build_app app-compensation-noop

# Capture one real instant only after the immutable build variants exist, then reuse it
# for every fresh-JVM prerequisite fixture. Callers may supply an explicit replay value.
if [[ -z "$FIXTURE_NOW" ]]; then
  FIXTURE_NOW="$(date -u +%Y-%m-%dT%H:%M:%S.000)"
fi

record_tree() {
  local scope="$1"
  local root="$2"
  find "$root" -type f \
    ! -path '*/target/*' \
    ! -path '*/.git/*' \
    ! -path '*/logs/*' \
    ! -name '.git' \
    ! -name 'classpath.txt' \
    -print0 | LC_ALL=C sort -z | while IFS= read -r -d '' path; do
      relative="${path#"$root"/}"
      printf '%s\t%s\t%s\n' "$scope" "$relative" "$(sha256sum "$path" | awk '{print $1}')"
    done
}

SOURCE_CONTENT_MANIFEST="$OUT_DIR/source-content-manifest.tsv"
{
  printf 'scope\tpath\tsha256\n'
  record_tree source-simulator /workspace/simulator
  record_tree build-simulator "$BUILD_DIR/simulator"
  record_tree source-verifiers /verifiers
  record_tree build-verifiers "$BUILD_DIR/verifiers"
  record_tree source-app /applications/quizzes
  record_tree app-base "$BUILD_DIR/app-base"
  record_tree app-question-repaired "$BUILD_DIR/app-question-repaired"
  record_tree app-compensation-noop "$BUILD_DIR/app-compensation-noop"
} > "$SOURCE_CONTENT_MANIFEST"

app_cp() {
  local app_dir="$1"
  printf '%s' "$BUILD_DIR/$app_dir/target/classes:$BUILD_DIR/$app_dir/target/test-classes:$BUILD_DIR/verifiers/target/classes:$(tr -d '\n' < "$BUILD_DIR/$app_dir/classpath.txt"):$(tr -d '\n' < "$BUILD_DIR/verifiers-classpath.txt")"
}

selection() {
  python3 - "$PACKAGE_DIR/selection.json" "$1" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as stream:
    print(json.load(stream)["cases"][sys.argv[2]]["faultScenarioId"])
PY
}

run_case() {
  local case_id="$1"
  local selection_key="$2"
  local app_dir="$3"
  local build_variant="$4"
  local observer_mode="$5"
  local java_option="${6:-}"
  local report="$OUT_DIR/$case_id.execution.json"
  local impact_v1="$OUT_DIR/$case_id.impact-v1.json"
  local witness="$OUT_DIR/$case_id.state-witness.json"
  local cp
  cp="$(app_cp "$app_dir")"
  local scenario_id
  scenario_id="$(selection "$selection_key")"
  local started_ns
  started_ns="$(date +%s%N)"
  local java_args=(
    -Xmx1536m -XX:MaxMetaspaceSize=512m
    -Dmicroservices.simulator.event-replay.enabled=true
    -Dimpact.v2.fixture-now="$FIXTURE_NOW"
    -Dimpact.v2.qualification.case="$case_id"
    -Dimpact.v2.qualification.build="$build_variant"
    -Dimpact.v2.qualification.source-revision="$SOURCE_REVISION"
    -Dimpact.v2.qualification.witness-output="$witness"
  )
  if [[ -n "$java_option" ]]; then
    java_args+=("$java_option")
  fi
  unset SPRING_PROFILES SPRING_PROFILES_ACTIVE
  java "${java_args[@]}" -cp "$cp" \
    pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
    --spring-application-class pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator \
    --spring-profiles test,sagas,local \
    --application-base quizzes \
    --application-id quizzes \
    --maven-profile test-sagas \
    --package-path "$PACKAGE_DIR/scenario-catalog-manifest.json" \
    --fault-scenario-id "$scenario_id" \
    --output-path "$report" \
    --impact-output-path "$impact_v1" \
    --verifiers.application.enabled=false \
    --server.port=0 > "$OUT_DIR/$case_id.log" 2>&1
  local duration_ns=$(( $(date +%s%N) - started_ns ))
  test -s "$witness"

  local sidecar="${report%.json}${IMPACT_V2_SIDECAR_SUFFIX}"
  test -s "$sidecar"
  python3 - "$sidecar" "$case_id" "$selection_key" "$scenario_id" "$build_variant" \
    "$observer_mode" "$duration_ns" "$report" "$impact_v1" "$witness" >> "$OUT_DIR/cases.tsv" <<'PY'
import json, pathlib, sys
sidecar = pathlib.Path(sys.argv[1])
with sidecar.open(encoding="utf-8") as stream:
    impact = json.load(stream)
fields = list(sys.argv[2:8])
fields.extend(str(pathlib.Path(value).stat().st_size) for value in sys.argv[8:11])
fields.append(str(sidecar.stat().st_size))
for key in ("baseline", "finalState", "committedWrites", "eventDeliveries", "coverageGaps"):
    value = impact.get(key)
    fields.append(str(len(value) if isinstance(value, list) else 0))
print("\t".join(fields))
PY
}

printf 'caseId\tselectionKey\tfaultScenarioId\tbuildVariant\tobserverMode\tdurationNanos\texecutionBytes\timpactV1Bytes\twitnessBytes\timpactV2Bytes\tbaselineCount\tfinalStateCount\tcommittedWriteCount\teventDeliveryCount\tcoverageGapCount\n' > "$OUT_DIR/cases.tsv"
# Every call starts a new JVM and therefore a fresh in-memory H2 database.
run_case deleted-assigned deletedDependencyAssigned app-base current on
run_case deleted-unassigned deletedDependencyUnassigned app-base current on
run_case residual-assigned-normal failedUpdateAssigned app-base current on
run_case residual-assigned-noop failedUpdateAssigned app-compensation-noop controlled-noop-compensation on
run_case residual-unassigned-normal failedUpdateUnassigned app-base current on
run_case residual-unassigned-noop failedUpdateUnassigned app-compensation-noop controlled-noop-compensation on
run_case event-current unresolvedQuestionEvent app-base current on
run_case event-repaired unresolvedQuestionEvent app-question-repaired controlled-registerChanged-repair on

run_case deleted-assigned-observer-off deletedDependencyAssigned app-base current off "$IMPACT_V2_DISABLED_JAVA_ARG"
run_case residual-assigned-normal-observer-off failedUpdateAssigned app-base current off "$IMPACT_V2_DISABLED_JAVA_ARG"
run_case event-current-observer-off unresolvedQuestionEvent app-base current off "$IMPACT_V2_DISABLED_JAVA_ARG"

{
  printf 'sourceRevision=%s\n' "$SOURCE_REVISION"
  printf 'fixtureNow=%s\n' "$FIXTURE_NOW"
  printf 'originalQuizServiceSha256=%s\n' "$QUIZ_SERVICE_SHA"
  printf 'repairedQuizServiceSha256=%s\n' "$REPAIRED_QUIZ_SERVICE_SHA"
  printf 'quizRepairPatchSha256=%s\n' "$QUIZ_PATCH_SHA"
  printf 'originalUpdateTournamentSagaSha256=%s\n' "$UPDATE_SAGA_SHA"
  printf 'mutantUpdateTournamentSagaSha256=%s\n' "$MUTANT_UPDATE_SAGA_SHA"
  printf 'compensationMutantPatchSha256=%s\n' "$COMP_PATCH_SHA"
  printf 'providerSourceSha256=%s\n' "$(sha256sum "$PROVIDER_SOURCE" | awk '{print $1}')"
  printf 'fixtureGeneratorSha256=%s\n' "$(sha256sum "$EXPERIMENT_ROOT/generate-fixture.groovy" | awk '{print $1}')"
  printf 'runnerSha256=%s\n' "$(sha256sum "$EXPERIMENT_ROOT/run.sh" | awk '{print $1}')"
  printf 'sourceContentManifestSha256=%s\n' "$(sha256sum "$SOURCE_CONTENT_MANIFEST" | awk '{print $1}')"
  find "$PACKAGE_DIR" -maxdepth 1 -type f -print0 | sort -z | while IFS= read -r -d '' path; do
    printf '%s  %s\n' "$(sha256sum "$path" | awk '{print $1}')" "${path#"$OUT_DIR"/}"
  done
  find "$OUT_DIR" -maxdepth 1 -type f \( -name '*.json' -o -name '*.tsv' \) -print0 | sort -z | while IFS= read -r -d '' path; do
    printf '%s  %s\n' "$(sha256sum "$path" | awk '{print $1}')" "${path#"$OUT_DIR"/}"
  done
} > "$OUT_DIR/artifact-hashes.txt"

python3 "$EXPERIMENT_ROOT/validate.py" "$OUT_DIR" --output "$OUT_DIR/validation.json"
echo "ImpactV2 persisted qualification passed: $OUT_DIR"
