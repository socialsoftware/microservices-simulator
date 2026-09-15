#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: generate-bounded-package.sh \
  --runtime RUNTIME_JSON --prepared-build DIR --source-test FILE --output DIR \
  --source-class FQN --source-method NAME --saga FQN --saga FQN \
  --max-workloads N --max-input-variants N --max-schedules N \
  --recovery-cap N --seed N --allow-type-only-fallback true|false

All paths are host paths. RUNTIME_JSON supplies the immutable Docker image. DIR and
FILE must be under verifiers/target and the repository respectively. The source test
is mounted over the frozen prepared build; generation remains source-derived.
EOF
}

runtime= prepared_build= source_test= output= source_class= source_method=
max_workloads= max_inputs= max_schedules= recovery_cap= seed= fallback=
sagas=()
while (($#)); do
  case "$1" in
    --runtime) runtime=$2; shift 2 ;;
    --prepared-build) prepared_build=$2; shift 2 ;;
    --source-test) source_test=$2; shift 2 ;;
    --output) output=$2; shift 2 ;;
    --source-class) source_class=$2; shift 2 ;;
    --source-method) source_method=$2; shift 2 ;;
    --saga) sagas+=("$2"); shift 2 ;;
    --max-workloads) max_workloads=$2; shift 2 ;;
    --max-input-variants) max_inputs=$2; shift 2 ;;
    --max-schedules) max_schedules=$2; shift 2 ;;
    --recovery-cap) recovery_cap=$2; shift 2 ;;
    --seed) seed=$2; shift 2 ;;
    --allow-type-only-fallback) fallback=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 2 ;;
  esac
done

for value in runtime prepared_build source_test output source_class source_method \
  max_workloads max_inputs max_schedules recovery_cap seed fallback; do
  [[ -n ${!value} ]] || { echo "Missing --${value//_/-}" >&2; exit 2; }
done
[[ ${#sagas[@]} -eq 2 ]] || { echo "Exactly two --saga arguments are required" >&2; exit 2; }
[[ $fallback == true || $fallback == false ]] || { echo "Fallback must be true or false" >&2; exit 2; }

repo=$(cd "$(dirname "$0")/../../.." && pwd)
target="$repo/verifiers/target"
experiment="$repo/verifiers/experiments/lost-copied-update"
runtime=$(cd "$(dirname "$runtime")" && pwd)/$(basename "$runtime")
prepared_build=$(cd "$prepared_build" && pwd)
source_test=$(cd "$(dirname "$source_test")" && pwd)/$(basename "$source_test")
output_parent=$(cd "$(dirname "$output")" && pwd)
output="$output_parent/$(basename "$output")"
case "$prepared_build" in "$target"/*) ;; *) echo "Prepared build must be under $target" >&2; exit 2;; esac
case "$output" in "$target"/*) ;; *) echo "Output must be under $target" >&2; exit 2;; esac
[[ ! -e $output ]] || { echo "Output already exists: $output" >&2; exit 2; }

image=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["image"])' "$runtime")
[[ $image == sha256:* ]] || { echo "Runtime image is not immutable" >&2; exit 2; }
build_container=/reports/${prepared_build#"$target"/}
out_container=/reports/${output#"$target"/}
test_relative=src/test/groovy/${source_class//.//}.groovy
test_container="$build_container/source/quizzes/$test_relative"
mkdir -p "$output/classes"

docker run --rm --network none \
  -v "$target:/reports" \
  -v "$experiment:/experiment:ro" \
  -v "$source_test:$test_container:ro" \
  --entrypoint bash "$image" -c '
    set -euo pipefail
    build=$1; out=$2; shift 2
    cp_value="$build/source/quizzes/target/classes:$build/source/quizzes/target/test-classes:/reports/classes:$(cat "$build/quizzes-classpath.txt"):$(cat "$build/verifiers-classpath.txt")"
    javac -cp "$cp_value" -d "$out/classes" /experiment/GenerateLostCopiedUpdatePackage.java
    java -Xmx2g -cp "$out/classes:$cp_value" GenerateLostCopiedUpdatePackage "$@"
  ' bash "$build_container" "$out_container" \
    "$build_container/source" quizzes "$out_container/package" \
    "$source_class" "$source_method" "${sagas[0]},${sagas[1]}" \
    "$max_workloads" "$max_inputs" "$max_schedules" "$recovery_cap" "$seed" "$fallback"
