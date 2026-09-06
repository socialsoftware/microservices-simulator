#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
EXPERIMENT_DIR="$ROOT_DIR/verifiers/experiments/impact-v2-broader"
: "${PACKAGE_PATH:?PACKAGE_PATH is required}"
: "${BROADER_SELECTION_PATH:?BROADER_SELECTION_PATH is required}"
: "${OUTPUT_DIR:?OUTPUT_DIR is required}"
: "${SOURCE_REVISION:?SOURCE_REVISION is required}"
: "${SOURCE_TREE_STATE:?SOURCE_TREE_STATE is required}"
: "${MAX_PARALLEL:=2}"
: "${PREPARE_CLASSES:=true}"
: "${PREPARED_BUILD_DIR:=}"
: "${ATTEMPT_JAVA_TOOL_OPTIONS:=-Xmx1536m -XX:MaxMetaspaceSize=512m}"
: "${MEDIUM_MEM_LIMIT:=4g}"
: "${MEDIUM_MEM_RESERVATION:=1g}"
: "${MEDIUM_CPUS:=5.0}"

[[ "$MAX_PARALLEL" =~ ^[12]$ ]] || { echo "MAX_PARALLEL must be 1 or 2" >&2; exit 2; }
[[ "$PREPARE_CLASSES" == true || "$PREPARE_CLASSES" == false ]] || {
  echo "PREPARE_CLASSES must be true or false" >&2; exit 2;
}

PACKAGE_PATH=$(realpath "$PACKAGE_PATH")
BROADER_SELECTION_PATH=$(realpath "$BROADER_SELECTION_PATH")
mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR=$(realpath "$OUTPUT_DIR")
REPORT_ROOT=$(realpath "$ROOT_DIR/verifiers/target")
for path in "$PACKAGE_PATH" "$BROADER_SELECTION_PATH" "$OUTPUT_DIR"; do
  case "$path" in "$REPORT_ROOT"/*) ;; *) echo "$path must be under $REPORT_ROOT" >&2; exit 2;; esac
done
if find "$OUTPUT_DIR" -mindepth 1 -print -quit | grep -q .; then
  echo "Refusing non-empty output directory: $OUTPUT_DIR" >&2
  exit 2
fi

mkdir -p "$OUTPUT_DIR/historical/attempts" "$OUTPUT_DIR/broader/attempts"
if [[ -z "$PREPARED_BUILD_DIR" ]]; then
  PREPARED_BUILD_DIR="$OUTPUT_DIR/prepared-build"
fi
mkdir -p "$PREPARED_BUILD_DIR"
PREPARED_BUILD_DIR=$(realpath "$PREPARED_BUILD_DIR")
case "$PREPARED_BUILD_DIR" in
  "$REPORT_ROOT"/*) ;; *) echo "$PREPARED_BUILD_DIR must be under $REPORT_ROOT" >&2; exit 2;;
esac
python3 "$EXPERIMENT_DIR/qualification.py" plan --manifest "$PACKAGE_PATH" \
  --historical "$EXPERIMENT_DIR/historical-baseline.json" \
  --output-json "$OUTPUT_DIR/historical/plan.json" --output-tsv "$OUTPUT_DIR/historical/plan.tsv"

package_dir=$(dirname "$PACKAGE_PATH")
(cd "$package_dir" && sha256sum *.json *.jsonl) > "$OUTPUT_DIR/package-before.sha256"
{
  printf 'scope\tpath\tsha256\n'
  for scope in simulator verifiers applications/quizzes; do
    find "$ROOT_DIR/$scope" -type f ! -path '*/target/*' ! -path '*/.git/*' ! -path '*/logs/*' \
      ! -path '*/experiments/*' \
      ! -path '*/__pycache__/*' ! -name '*.pyc' -print0 |
      LC_ALL=C sort -z | while IFS= read -r -d '' path; do
        printf '%s\t%s\t%s\n' "$scope" "${path#"$ROOT_DIR/$scope"/}" "$(sha256sum "$path" | awk '{print $1}')"
      done
  done
} > "$OUTPUT_DIR/source-content-manifest.tsv"

build_output_container=/reports/${PREPARED_BUILD_DIR#"$REPORT_ROOT"/}
if [[ "$PREPARE_CLASSES" == true ]]; then
  if [[ -e "$PREPARED_BUILD_DIR/status" ]]; then
    echo "Refusing to replace an existing prepared build" >&2
    exit 2
  fi
  docker compose -p microservices-simulator -f "$ROOT_DIR/docker-compose.yml" run --rm --no-deps --pull never -T \
    -e BUILD_OUTPUT_DIR="$build_output_container" -e JAVA_TOOL_OPTIONS="$ATTEMPT_JAVA_TOOL_OPTIONS" \
    scenario-executor /verifiers/experiments/impact-v2-broader/prepare-build.sh \
    > "$OUTPUT_DIR/prepare-build.log" 2>&1
  sha256sum "$OUTPUT_DIR/source-content-manifest.tsv" | awk '{print $1}' \
    > "$PREPARED_BUILD_DIR/source-content-manifest.sha256"
fi
test "$(cat "$PREPARED_BUILD_DIR/status")" = ready
expected_source_hash=$(sha256sum "$OUTPUT_DIR/source-content-manifest.tsv" | awk '{print $1}')
test "$(cat "$PREPARED_BUILD_DIR/source-content-manifest.sha256")" = "$expected_source_hash"

python3 - "$OUTPUT_DIR/all-attempts.tsv" "$OUTPUT_DIR/historical/plan.json" "$BROADER_SELECTION_PATH" <<'PY'
import json, pathlib, sys
out, historical, broader = map(pathlib.Path, sys.argv[1:])
rows=[]
for row in json.load(historical.open())["rows"]:
    rows.append(["historical", f"historical-{row['faultScenarioId'][:16]}", row["faultScenarioId"], row["vector"]])
for row in json.load(broader.open())["rows"]:
    rows.append(["broader", row["caseId"], row["faultScenarioId"], row["faultVector"]])
assert len(rows)==77 and len({(r[0],r[1]) for r in rows})==77
out.write_text("".join("\t".join(r)+"\n" for r in rows))
PY

package_container=/reports/${PACKAGE_PATH#"$REPORT_ROOT"/}
output_container=/reports/${OUTPUT_DIR#"$REPORT_ROOT"/}
workload_id=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["workloadPlanId"])' "$OUTPUT_DIR/historical/plan.json")
export ROOT_DIR package_container output_container workload_id SOURCE_REVISION SOURCE_TREE_STATE \
  ATTEMPT_JAVA_TOOL_OPTIONS MEDIUM_MEM_LIMIT MEDIUM_MEM_RESERVATION MEDIUM_CPUS build_output_container
run_one() {
  local cohort=$1 case_id=$2 scenario_id=$3 vector=$4
  local host_dir="$OUTPUT_DIR/$cohort/attempts/$([[ "$cohort" == historical ]] && echo "$scenario_id" || echo "$case_id")"
  local container_dir="$output_container/$cohort/attempts/$([[ "$cohort" == historical ]] && echo "$scenario_id" || echo "$case_id")"
  mkdir -p "$host_dir"
  local args=(docker compose -p microservices-simulator -f "$ROOT_DIR/docker-compose.yml" run --rm --no-deps --pull never -T
    -e BUILD_OUTPUT_DIR="$build_output_container" -e JAVA_TOOL_OPTIONS="$ATTEMPT_JAVA_TOOL_OPTIONS"
    -e PACKAGE_PATH="$package_container" -e FAULT_SCENARIO_ID="$scenario_id"
    -e EXECUTION_OUTPUT_PATH="$container_dir/execution.json" -e IMPACT_OUTPUT_PATH="$container_dir/impact-v1.json")
  args+=(-e RUNNER_KIND=generic)
  set +e
  "${args[@]}" scenario-executor /verifiers/experiments/impact-v2-broader/run-prepared.sh > "$host_dir/docker.log" 2>&1
  local exit_code=$?
  set -e
  printf '%s\n' "$exit_code" > "$host_dir/process-exit-code"
  local required=("$host_dir/execution.json" "$host_dir/impact-v1.json" "$host_dir/execution.impact-v2.json")
  for artifact in "${required[@]}"; do
    if [[ ! -s "$artifact" ]]; then
      echo "Attempt $cohort/$case_id did not produce $artifact (exit $exit_code)" >&2
      return 2
    fi
  done
  return 0
}
export OUTPUT_DIR
export -f run_one
xargs -P "$MAX_PARALLEL" -n 4 bash -c 'run_one "$@"' _ < "$OUTPUT_DIR/all-attempts.tsv"

(cd "$package_dir" && sha256sum *.json *.jsonl) > "$OUTPUT_DIR/package-after.sha256"
diff -u "$OUTPUT_DIR/package-before.sha256" "$OUTPUT_DIR/package-after.sha256" > "$OUTPUT_DIR/package-execution.diff"
python3 "$EXPERIMENT_DIR/qualification.py" aggregate --plan "$OUTPUT_DIR/historical/plan.json" \
  --attempt-dir "$OUTPUT_DIR/historical/attempts" --output-json "$OUTPUT_DIR/historical/results.json" \
  --output-csv "$OUTPUT_DIR/historical/results.csv"
python3 "$EXPERIMENT_DIR/summarize-broader.py" "$BROADER_SELECTION_PATH" \
  "$OUTPUT_DIR/broader/attempts" "$OUTPUT_DIR/broader/results.json" --manifest "$PACKAGE_PATH"
sha256sum "$OUTPUT_DIR/source-content-manifest.tsv" "$OUTPUT_DIR/historical/plan.json" \
  "$BROADER_SELECTION_PATH" > "$OUTPUT_DIR/run-inputs.sha256"
