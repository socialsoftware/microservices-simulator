#!/usr/bin/env bash
# Runs the Locust functionality tests (applications/quizzes/src/test/locust/tests/functionality tests/) on the 9
# deployments, in each tx-mode. Every run restarts the deployment (compose down/up, health check, settle time), runs
# applications/quizzes/src/test/locust/run_functionality_tests.py --tests <file>, and saves the compose logs.
#
# Output: <results-root>/locust/<mode>/<test>/runs.csv and summary.csv, plus per-run Locust and compose logs.

set -u

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
QUIZZES_DIR="${PROJECT_ROOT}/applications/quizzes"

SERVER="localhost"
PORT="8080"
PROTOCOL="http"
REPETITIONS=5
TX_MODE="sagas"
WAIT_TIMEOUT_SECONDS=120
STARTUP_SETTLE_SECONDS=15
COMPOSE_LOG_TAIL=2000

DEPLOYMENT_NAMES=(
  "centralized-local"
  "centralized-stream"
  "centralized-grpc"
  "distributed-stream"
  "distributed-grpc"
  "centralized-stream-distributed-version"
  "centralized-grpc-distributed-version"
  "distributed-stream-distributed-version"
  "distributed-grpc-distributed-version"
)

configure_deployment_commands() {
  DEPLOYMENT_COMMANDS=(
    "env TX_MODE=${TX_MODE} docker compose up quizzes-local -d --wait"
    "env TX_MODE=${TX_MODE} COMM_LAYER=stream docker compose up quizzes-remote version-service -d --wait"
    "env TX_MODE=${TX_MODE} COMM_LAYER=grpc docker compose up quizzes-remote version-service -d --wait"
    "env TX_MODE=${TX_MODE} COMM_LAYER=stream docker compose up answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service gateway version-service -d --wait"
    "env TX_MODE=${TX_MODE} COMM_LAYER=grpc docker compose up answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service gateway version-service -d --wait"
    "env TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=stream docker compose up quizzes-remote -d --wait"
    "env TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=grpc docker compose up quizzes-remote -d --wait"
    "env TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=stream docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d --wait"
    "env TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=grpc docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d --wait"
  )
}

log() {
  echo "[$(date +%H:%M:%S)] $*"
}

debug() {
  log "[DEBUG] $*"
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

compose_down() {
  (
    cd "${QUIZZES_DIR}" || exit 1
    docker compose down --remove-orphans >/dev/null 2>&1 || true
  )
}

start_deployment() {
  local command="$1"
  (
    cd "${QUIZZES_DIR}" || exit 1
    eval "${command}"
  )
}

capture_compose_logs() {
  local output_file="$1"

  (
    cd "${QUIZZES_DIR}" || exit 1
    docker compose logs --no-color --timestamps --tail "${COMPOSE_LOG_TAIL}"
  ) > "${output_file}" 2>&1 || true
}

wait_for_http_ready() {
  local url="${PROTOCOL}://${SERVER}:${PORT}/actuator/health"
  local elapsed=0

  debug "Waiting for health endpoint: ${url} (timeout=${WAIT_TIMEOUT_SECONDS}s)"
  while [[ "${elapsed}" -lt "${WAIT_TIMEOUT_SECONDS}" ]]; do
    if body="$(curl -fsS "${url}" 2>/dev/null)"; then
      if echo "${body}" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
        debug "Health endpoint reported UP after ${elapsed}s"
        return 0
      fi
      if [[ "${body}" == *"UP"* ]]; then
        debug "Health endpoint contained UP after ${elapsed}s"
        return 0
      fi
    fi
    if (( elapsed == 0 || elapsed % 10 == 0 )); then
      debug "Health check still pending (${elapsed}s elapsed)"
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done

  debug "Health check timed out after ${WAIT_TIMEOUT_SECONDS}s"
  return 1
}

LOCUST_DIR="${QUIZZES_DIR}/src/test/locust"
PYTHON="${PYTHON:-${LOCUST_DIR}/.venv/bin/python}"
RUN_TIME="120s"

ALL_TESTS=(
  "5a-updateStudentName-addParticipant-processUpdateNameEvent"
  "5b-addParticipant-updateStudentName-processUpdateNameEvent"
  "5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent"
  "5d-addParticipant1-updateStudentName-processUpdateNameEvent1-addParticipant2-processUpdateNameEvent2"
  "8-5-update-tournament-concurrent-intention-pass"
  "8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first"
  "8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last"
  "8-8-update-execution-student-add-participant-process-event-add-participant"
  "8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first"
  "8-10-concurrent-delete-tournament-add-participant"
  "concurrentAddparticipant"
  "concurrentAddUpdateparticipant"
  "concurrent-addCreatorAsParticipant-updateCreatorName-invBreak"
  "test_Multiple_Tests"
  "DeleteTournament"
  "DeleteUserTest"
  "Getquiz"
)

TESTS_FILTER=""
TX_MODES="sagas,tcc"
DEPLOYMENTS_FILTER=""
RESULTS_ROOT=""

locust_usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  --tests a,b               Test name prefixes (default: all 17)
  --tx-modes sagas,tcc      Transaction modes (default: ${TX_MODES})
  --deployments a,b         Only these deployments (default: all 9)
  --repetitions N           Runs per deployment (default: ${REPETITIONS})
  --results-root DIR        Root of the results (default: jmeter-results/functionality_<timestamp>)
  --run-time T              Locust safety cap per run (default: ${RUN_TIME})
  --wait-timeout S          Health-check timeout after startup (default: ${WAIT_TIMEOUT_SECONDS})
  --startup-settle S        Extra wait after the health check (default: ${STARTUP_SETTLE_SECONDS})
  --compose-log-tail N      Compose log lines saved per run (default: ${COMPOSE_LOG_TAIL})
Environment: PYTHON (default: ${PYTHON})
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tests) TESTS_FILTER="$2"; shift 2 ;;
    --tx-modes) TX_MODES="$2"; shift 2 ;;
    --deployments) DEPLOYMENTS_FILTER="$2"; shift 2 ;;
    --repetitions) REPETITIONS="$2"; shift 2 ;;
    --results-root) RESULTS_ROOT="$2"; shift 2 ;;
    --run-time) RUN_TIME="$2"; shift 2 ;;
    --wait-timeout) WAIT_TIMEOUT_SECONDS="$2"; shift 2 ;;
    --startup-settle) STARTUP_SETTLE_SECONDS="$2"; shift 2 ;;
    --compose-log-tail) COMPOSE_LOG_TAIL="$2"; shift 2 ;;
    -h|--help) locust_usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; locust_usage; exit 1 ;;
  esac
done

[[ -x "${PYTHON}" ]] || fail "Python not found at ${PYTHON} (set PYTHON=...)"
command -v docker >/dev/null 2>&1 || fail "docker is required"
command -v curl >/dev/null 2>&1 || fail "curl is required"

RESULTS_ROOT="${RESULTS_ROOT:-${PROJECT_ROOT}/jmeter-results/functionality_$(date +%Y%m%d_%H%M%S)}"
mkdir -p "${RESULTS_ROOT}"
RESULTS_ROOT="$(cd "${RESULTS_ROOT}" && pwd)"

selected=()
for t in "${ALL_TESTS[@]}"; do
  if [[ -z "${TESTS_FILTER}" ]]; then
    selected+=("$t")
  else
    for prefix in ${TESTS_FILTER//,/ }; do
      if [[ "$t" == "${prefix}"* ]]; then selected+=("$t"); break; fi
    done
  fi
done
[[ ${#selected[@]} -gt 0 ]] || fail "No test matches ${TESTS_FILTER}"

run_one_test() {
  local test="$1" out="$2"
  local runs_csv="${out}/runs.csv" summary_csv="${out}/summary.csv"
  echo "deployment,run,exit,run_pass,duration_s,failure,log_file,compose_logs_file" > "${runs_csv}"
  echo "deployment,runs,pass_runs,run_pass_rate" > "${summary_csv}"

  local i deployment command run run_prefix log_file compose_logs_file result_csv exit_code run_pass duration failure
  for i in "${!DEPLOYMENT_NAMES[@]}"; do
    deployment="${DEPLOYMENT_NAMES[$i]}"
    command="${DEPLOYMENT_COMMANDS[$i]}"
    if [[ -n "${DEPLOYMENTS_FILTER}" && ",${DEPLOYMENTS_FILTER}," != *",${deployment},"* ]]; then
      continue
    fi
    log "[${deployment}] ${TX_MODE} ${test}"
    debug "[${deployment}] Compose command: ${command}"

    for run in $(seq 1 "${REPETITIONS}"); do
      run_prefix="${deployment}__run${run}"
      log_file="${out}/${run_prefix}.locust.log"
      compose_logs_file="${out}/${run_prefix}.compose.log"
      result_csv="${out}/${run_prefix}.result.csv"

      log "[${deployment}] Run ${run}/${REPETITIONS}: restarting deployment"
      compose_down
      if ! start_deployment "${command}"; then
        log "[${deployment}] Run ${run}: deployment startup failed"
        capture_compose_logs "${compose_logs_file}"
        echo "${deployment},${run},1,0,0,deployment startup failed,${log_file},${compose_logs_file}" >> "${runs_csv}"
        continue
      fi
      if ! wait_for_http_ready; then
        log "[${deployment}] Run ${run}: health check timeout"
        capture_compose_logs "${compose_logs_file}"
        echo "${deployment},${run},1,0,0,health check timeout,${log_file},${compose_logs_file}" >> "${runs_csv}"
        continue
      fi
      if [[ "${STARTUP_SETTLE_SECONDS}" -gt 0 ]]; then
        log "[${deployment}] Run ${run}: waiting ${STARTUP_SETTLE_SECONDS}s for service discovery convergence"
        sleep "${STARTUP_SETTLE_SECONDS}"
      fi

      log "[${deployment}] Run ${run}: executing Locust"
      exit_code=0
      "${PYTHON}" "${LOCUST_DIR}/run_functionality_tests.py" --tests "${test}.py" --run-time "${RUN_TIME}" \
        --verbose --results-file "${result_csv}" > "${log_file}" 2>&1 || exit_code=$?

      run_pass=0
      [[ "${exit_code}" -eq 0 ]] && run_pass=1
      duration=$(awk -F',' 'NR==2 { print $3 }' "${result_csv}" 2>/dev/null)
      failure=$("${PYTHON}" -c 'import csv,sys
rows=list(csv.DictReader(open(sys.argv[1])))
print((rows[0]["failure"] if rows else "no result").replace(",",";").replace("\n"," ")[:500])' "${result_csv}" 2>/dev/null)
      log "[${deployment}] Run ${run}: Locust exit=${exit_code}, run_pass=${run_pass}"
      if [[ "${run_pass}" -eq 0 ]]; then
        echo "${failure}" | sed 's/^/[LOCUST] /'
      fi

      capture_compose_logs "${compose_logs_file}"
      echo "${deployment},${run},${exit_code},${run_pass},${duration:-0},${failure},${log_file},${compose_logs_file}" >> "${runs_csv}"
    done

    awk -F',' -v dep="${deployment}" '$1==dep { r++; p+=$4 } END {
      printf "%s,%d,%d,%.2f\n", dep, r, p, (r==0 ? 0 : (p*100.0)/r) }' "${runs_csv}" >> "${summary_csv}"
  done
}

overall=0
for mode in ${TX_MODES//,/ }; do
  TX_MODE="${mode}"
  configure_deployment_commands
  for test in "${selected[@]}"; do
    out="${RESULTS_ROOT}/locust/${mode}/${test}"
    mkdir -p "${out}"
    log "Locust ${mode} ${test} -> ${out}"
    run_one_test "${test}" "${out}"
    if awk -F',' 'NR>1 && $4==0 { bad=1 } END { exit !bad }' "${out}/runs.csv"; then overall=1; fi
  done
done
compose_down

log "Locust results under ${RESULTS_ROOT}/locust"
exit "${overall}"
