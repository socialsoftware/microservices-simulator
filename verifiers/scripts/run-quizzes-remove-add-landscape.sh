#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
PYTHON_TOOL="$ROOT_DIR/verifiers/scripts/quizzes-remove-add-landscape.py"
ATTEMPT_RUNNER=/verifiers/scripts/run-quizzes-remove-add-benchmark.sh

required=(PACKAGE_PATH WORKLOAD_PLAN_ID OUTPUT_DIR SOURCE_REVISION SOURCE_TREE_STATE)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "$name is required" >&2
    exit 2
  fi
done
: "${MAX_PARALLEL:=1}"
: "${RESUME:=false}"
: "${ATTEMPT_JAVA_TOOL_OPTIONS:=-Xmx1000m}"
if ! [[ "$MAX_PARALLEL" =~ ^[1-9][0-9]*$ ]]; then
  echo "MAX_PARALLEL must be a positive integer" >&2
  exit 2
fi
if [[ "$RESUME" != "true" && "$RESUME" != "false" ]]; then
  echo "RESUME must be exactly 'true' or 'false'" >&2
  exit 2
fi
if [[ -z "$ATTEMPT_JAVA_TOOL_OPTIONS" ]]; then
  echo "ATTEMPT_JAVA_TOOL_OPTIONS must not be empty" >&2
  exit 2
fi

PACKAGE_PATH=$(realpath "$PACKAGE_PATH")
OUTPUT_DIR=$(mkdir -p "$OUTPUT_DIR" && cd "$OUTPUT_DIR" && pwd)
REPORT_ROOT=$(realpath "$ROOT_DIR/verifiers/target")
case "$PACKAGE_PATH" in
  "$REPORT_ROOT"/*) ;;
  *) echo "PACKAGE_PATH must be under $REPORT_ROOT for the Docker /reports mount" >&2; exit 2 ;;
esac
case "$OUTPUT_DIR" in
  "$REPORT_ROOT"/*) ;;
  *) echo "OUTPUT_DIR must be under $REPORT_ROOT for the Docker /reports mount" >&2; exit 2 ;;
esac
PACKAGE_CONTAINER="/reports/${PACKAGE_PATH#"$REPORT_ROOT"/}"
OUTPUT_CONTAINER="/reports/${OUTPUT_DIR#"$REPORT_ROOT"/}"
PLAN_PATH="$OUTPUT_DIR/landscape-plan.tsv"
ATTEMPT_DIR="$OUTPUT_DIR/attempts"
mkdir -p "$ATTEMPT_DIR"

python3 "$PYTHON_TOOL" plan \
  --manifest "$PACKAGE_PATH" \
  --workload-plan-id "$WORKLOAD_PLAN_ID" \
  --output "$PLAN_PATH"

sha256sum "$(dirname "$PACKAGE_PATH")"/*.json "$(dirname "$PACKAGE_PATH")"/*.jsonl \
  > "$OUTPUT_DIR/package-before-execution.sha256"

export WORKLOAD_PLAN_ID SOURCE_REVISION SOURCE_TREE_STATE MAX_PARALLEL RESUME ATTEMPT_JAVA_TOOL_OPTIONS
python3 - "$OUTPUT_DIR/run-metadata.json" "$PACKAGE_PATH" "$PLAN_PATH" "$PYTHON_TOOL" \
  "$ROOT_DIR/verifiers/scripts/run-quizzes-remove-add-benchmark.sh" <<'PY'
import hashlib, json, os, pathlib, sys

def digest(path):
    return hashlib.sha256(pathlib.Path(path).read_bytes()).hexdigest()

metadata = {
    "schemaVersion": "microservices-simulator.quizzes-remove-add-landscape-run.v1",
    "command": "verifiers/scripts/run-quizzes-remove-add-landscape.sh",
    "composeService": "scenario-executor",
    "packageManifestPath": str(pathlib.Path(sys.argv[2]).resolve()),
    "packageManifestSha256": digest(sys.argv[2]),
    "workloadPlanId": os.environ["WORKLOAD_PLAN_ID"],
    "sourceRevision": os.environ["SOURCE_REVISION"],
    "sourceTreeState": os.environ["SOURCE_TREE_STATE"],
    "maxParallel": int(os.environ["MAX_PARALLEL"]),
    "resume": os.environ["RESUME"] == "true",
    "attemptJavaToolOptions": os.environ["ATTEMPT_JAVA_TOOL_OPTIONS"],
    "landscapePlanSha256": digest(sys.argv[3]),
    "toolSha256": {
        "aggregator": digest(sys.argv[4]),
        "attemptRunner": digest(sys.argv[5]),
    },
}
pathlib.Path(sys.argv[1]).write_text(json.dumps(metadata, indent=2) + "\n")
PY

docker compose -f "$ROOT_DIR/docker-compose.yml" build scenario-executor \
  > "$OUTPUT_DIR/docker-build.log" 2>&1

export ROOT_DIR PACKAGE_CONTAINER OUTPUT_CONTAINER WORKLOAD_PLAN_ID SOURCE_REVISION SOURCE_TREE_STATE ATTEMPT_DIR ATTEMPT_RUNNER RESUME ATTEMPT_JAVA_TOOL_OPTIONS
run_one() {
  local vector=$1 ordinal=$2 scenario_id=$3
  local host_dir="$ATTEMPT_DIR/$scenario_id"
  local container_dir="$OUTPUT_CONTAINER/attempts/$scenario_id"
  mkdir -p "$host_dir"
  if [[ -e "$host_dir/result.json" || -e "$host_dir/execution.json" || -e "$host_dir/impact.json" ]]; then
    if [[ "$RESUME" == "true" && -f "$host_dir/result.json" && -f "$host_dir/execution.json" && -f "$host_dir/impact.json" ]]; then
      echo "Keeping completed landscape attempt $scenario_id"
      return 0
    fi
    echo "Refusing to overwrite partial or existing landscape attempt $scenario_id" >&2
    return 2
  fi
  docker compose -f "$ROOT_DIR/docker-compose.yml" run --rm --no-deps \
    -e JAVA_TOOL_OPTIONS="$ATTEMPT_JAVA_TOOL_OPTIONS" \
    -e PACKAGE_PATH="$PACKAGE_CONTAINER" \
    -e WORKLOAD_PLAN_ID="$WORKLOAD_PLAN_ID" \
    -e FAULT_SCENARIO_ID="$scenario_id" \
    -e EXECUTION_OUTPUT_PATH="$container_dir/execution.json" \
    -e IMPACT_OUTPUT_PATH="$container_dir/impact.json" \
    -e RESULT_OUTPUT_PATH="$container_dir/result.json" \
    -e REPETITION=1 \
    -e RUNTIME_CONTEXT_ID="m3-$vector-$ordinal" \
    -e SOURCE_REVISION="$SOURCE_REVISION" \
    -e SOURCE_TREE_STATE="$SOURCE_TREE_STATE" \
    scenario-executor "$ATTEMPT_RUNNER" \
    > "$host_dir/docker.log" 2>&1
  python3 - "$host_dir/result.json" <<'PY'
import json, sys
result = json.load(open(sys.argv[1]))
print(f"{result['assignedVector']} {result['faultScenarioId']} "
      f"classification={result['classification']} validity={result['validity']}")
PY
}
export -f run_one

# xargs gives each selected FaultScenario a separate compose-run process and H2 database.
# Parallelism changes only wall-clock time; no application state is shared.
xargs -P "$MAX_PARALLEL" -n 3 bash -c 'run_one "$@"' _ < "$PLAN_PATH"

sha256sum "$(dirname "$PACKAGE_PATH")"/*.json "$(dirname "$PACKAGE_PATH")"/*.jsonl \
  > "$OUTPUT_DIR/package-after-execution.sha256"
diff -u "$OUTPUT_DIR/package-before-execution.sha256" "$OUTPUT_DIR/package-after-execution.sha256" \
  > "$OUTPUT_DIR/package-execution-hash.diff"

python3 "$PYTHON_TOOL" aggregate \
  --manifest "$PACKAGE_PATH" \
  --workload-plan-id "$WORKLOAD_PLAN_ID" \
  --attempt-dir "$ATTEMPT_DIR" \
  --output-json "$OUTPUT_DIR/quizzes-remove-add-landscape.json" \
  --output-csv "$OUTPUT_DIR/quizzes-remove-add-landscape.csv"

echo "Landscape evidence: $OUTPUT_DIR/quizzes-remove-add-landscape.json"
