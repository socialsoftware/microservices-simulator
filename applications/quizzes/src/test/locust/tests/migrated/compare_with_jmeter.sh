#!/usr/bin/env bash
# Runs each numbered JMeter thesis case and its Locust port, both on a freshly restarted app, and
# compares their outcomes. Temporary: delete it together with the jmeter/ folder (see MIGRATION.md).

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOCUST_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
QUIZZES_DIR="$(cd "${LOCUST_DIR}/../../.." && pwd)"
TEST_DIR="${QUIZZES_DIR}/jmeter/tournament/thesis-cases"

JMETER_BIN="${JMETER_BIN:-/opt/apache-jmeter-5.6.3/bin/jmeter}"
LOCUST_PYTHON="${LOCUST_PYTHON:-${LOCUST_DIR}/.venv/bin/python}"
DOCKER="${DOCKER:-}"
CONTAINER="quizzes-local"
APP_URL="http://localhost:8080"
TX_MODE="sagas"
START=0
TESTS=""
RESULTS_DIR=""
WAIT_TIMEOUT_SECONDS=300
LOCUST_RUN_TIME="180s"

# JMeter thesis case -> Locust port (see MIGRATION.md)
CASES=(5a 5b 5c 5d 8-5 8-6 8-7 8-8 8-9 8-10)
declare -A LOCUST_FILES=(
  [5a]="5a_update_name_then_add_participant_test.py"
  [5b]="5b_add_participant_then_update_name_test.py"
  [5c]="5c_add_participant_concurrent_update_name_test.py"
  [5d]="5d_update_name_processed_then_concurrent_add_participant_test.py"
  [8-5]="8_5_concurrent_update_tournament_test.py"
  [8-6]="8_6_add_creator_concurrent_update_name_processed_first_test.py"
  [8-7]="8_7_add_creator_concurrent_update_name_processed_last_test.py"
  [8-8]="8_8_update_name_then_add_creator_after_event_test.py"
  [8-9]="8_9_add_participant_concurrent_anonymize_processed_first_test.py"
  [8-10]="8_10_concurrent_remove_tournament_add_participant_test.py"
)

usage() {
  cat <<EOF
Usage: $0 [options]

Runs each numbered JMeter thesis case and then its Locust port, restarting the ${CONTAINER} container
before every run (the app recreates its database on start, which the JMeter tests need), and prints
whether both pass or fail alike.

Options:
  --tx-mode MODE        sagas or tcc, the profile the app must run with (default: ${TX_MODE})
  --start               build and start the ${CONTAINER} stack with --tx-mode first
  --tests LIST          comma separated subset, e.g. 5a,8-6 (default: all of ${CASES[*]})
  --results-dir PATH    where to keep the JMeter and Locust outputs (default: a new folder in /tmp)
  -h, --help            show this help

Environment:
  DOCKER         docker command (default: docker, or "sudo docker" when docker is not accessible)
  JMETER_BIN     JMeter binary (default: /opt/apache-jmeter-5.6.3/bin/jmeter)
  LOCUST_PYTHON  Python with Locust installed (default: ${LOCUST_DIR}/.venv/bin/python)

Examples:
  $0 --start --tx-mode sagas
  $0 --tx-mode tcc --tests 8-6,8-7
EOF
}

log() {
  echo "[$(date +%H:%M:%S)] $*"
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --tx-mode)
        [[ $# -ge 2 ]] || fail "--tx-mode needs a value"
        TX_MODE="$2"
        shift 2
        ;;
      --start)
        START=1
        shift
        ;;
      --tests)
        [[ $# -ge 2 ]] || fail "--tests needs a value"
        TESTS="$2"
        shift 2
        ;;
      --results-dir)
        [[ $# -ge 2 ]] || fail "--results-dir needs a value"
        RESULTS_DIR="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        fail "Unknown option: $1 (see --help)"
        ;;
    esac
  done

  [[ "${TX_MODE}" == "sagas" || "${TX_MODE}" == "tcc" ]] || fail "--tx-mode must be one of: sagas, tcc"

  if [[ -n "${TESTS}" ]]; then
    local all_cases="${CASES[*]}"
    IFS=',' read -r -a CASES <<< "${TESTS}"
    for case in "${CASES[@]}"; do
      [[ -n "${LOCUST_FILES[${case}]+x}" ]] || fail "Unknown test '${case}', expected one of: ${all_cases}"
    done
  fi

  if [[ -z "${RESULTS_DIR}" ]]; then
    RESULTS_DIR="/tmp/jmeter-vs-locust-${TX_MODE}-$(date +%Y%m%d_%H%M%S)"
  fi
}

validate_environment() {
  [[ -x "${JMETER_BIN}" ]] || fail "JMeter binary not executable: ${JMETER_BIN} (set JMETER_BIN)"
  [[ -x "${LOCUST_PYTHON}" ]] || fail "Python not found: ${LOCUST_PYTHON} (set LOCUST_PYTHON)"
  "${LOCUST_PYTHON}" -c "import locust" 2>/dev/null || fail "Locust is not installed for ${LOCUST_PYTHON}"
  command -v python3 >/dev/null || fail "python3 is needed to read the JMeter results"
  command -v curl >/dev/null || fail "curl is needed to wait for the app"

  if [[ -z "${DOCKER}" ]]; then
    DOCKER="docker"
    if ! docker info >/dev/null 2>&1; then
      log "docker is not accessible to $(id -un), using sudo"
      DOCKER="sudo docker"
      sudo -v || fail "sudo is needed to control the containers"
    fi
  fi
  mkdir -p "${RESULTS_DIR}" || fail "Cannot create ${RESULTS_DIR}"
}

compose_up() {
  log "Building and starting ${CONTAINER} with TX_MODE=${TX_MODE} (the first build takes a few minutes)"
  (
    cd "${QUIZZES_DIR}" || exit 1
    if [[ "${DOCKER}" == sudo* ]]; then
      sudo TX_MODE="${TX_MODE}" ${DOCKER#sudo } compose up -d --build "${CONTAINER}"
    else
      TX_MODE="${TX_MODE}" ${DOCKER} compose up -d --build "${CONTAINER}"
    fi
  ) > "${RESULTS_DIR}/compose.log" 2>&1 || fail "docker compose up failed, see ${RESULTS_DIR}/compose.log"
}

check_profile() {
  local profiles
  profiles="$(${DOCKER} inspect "${CONTAINER}" --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null \
    | sed -n 's/^SPRING_PROFILES_ACTIVE=//p')"
  [[ -n "${profiles}" ]] || fail "${CONTAINER} is not running, start it with --start"
  [[ ",${profiles}," == *",${TX_MODE},"* ]] \
    || fail "${CONTAINER} runs with profiles '${profiles}', not ${TX_MODE}. Use --start to restart it with --tx-mode ${TX_MODE}"
  log "${CONTAINER} runs with profiles ${profiles}"
}

wait_ready() {
  local deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))
  until [[ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "${APP_URL}/users/students")" == "200" ]]; do
    [[ ${SECONDS} -lt ${deadline} ]] || fail "The app did not answer on ${APP_URL} within ${WAIT_TIMEOUT_SECONDS}s"
    sleep 2
  done
  sleep 3
}

restart_app() {
  ${DOCKER} restart "${CONTAINER}" >/dev/null || fail "Could not restart ${CONTAINER}"
  sleep 2
  wait_ready
}

# Prints PASS, or FAIL with the failed samples, using the benchmark script's rule: exit 0 and no failed sample
jmeter_result() {
  python3 -I - "$1" "$2" <<'EOF'
import csv, sys
path, exit_code = sys.argv[1], int(sys.argv[2])
try:
    with open(path, newline="") as f:
        rows = list(csv.DictReader(f))
except FileNotFoundError:
    rows = []
failed = [r for r in rows if r.get("success") != "true"]
if exit_code == 0 and rows and not failed:
    print("PASS")
elif not rows:
    print(f"FAIL no samples recorded (JMeter exit {exit_code})")
else:
    print("FAIL " + " | ".join(
        f"{r['label']} [{r['responseCode']}] {r.get('failureMessage') or ''}".strip() for r in failed))
EOF
}

# Prints PASS, or FAIL with the failed checks, from the Locust port's "Test Succeeded!" / "Test failed:" lines
locust_result() {
  local output
  output="$(sed 's/\x1b\[[0-9;]*m//g' "$1")"
  if grep -q "Test failed:" <<< "${output}"; then
    echo "FAIL $(grep -o 'Test failed: .*' <<< "${output}" | sed 's/^Test failed: //' | paste -sd'|' | sed 's/|/ | /g')"
  elif grep -q "Test Succeeded!" <<< "${output}"; then
    echo "PASS"
  else
    echo "FAIL no result reported: $(grep -v '^[[:space:]]*$' <<< "${output}" | tail -1)"
  fi
}

run_jmeter() {
  local case="$1" jmx
  jmx="$(ls "${TEST_DIR}/${case}"-*.jmx 2>/dev/null | head -1)"
  [[ -n "${jmx}" ]] || { echo "FAIL no ${case}-*.jmx in ${TEST_DIR}"; return; }
  (
    cd "${RESULTS_DIR}" || exit 1
    "${JMETER_BIN}" -n -t "${jmx}" -l "${case}.jtl" -j "${case}.jmeter.log" \
      -Jserver=localhost -Jport=8080 -Jprotocol=http \
      -Jjmeter.save.saveservice.output_format=csv > "${case}.jmeter.out" 2>&1
  )
  jmeter_result "${RESULTS_DIR}/${case}.jtl" "$?"
}

run_locust() {
  local case="$1"
  (
    cd "${LOCUST_DIR}/tests" || exit 1
    "${LOCUST_PYTHON}" -m locust -f "migrated/${LOCUST_FILES[${case}]}" --headless --only-summary \
      -u 1 -r 1 --run-time "${LOCUST_RUN_TIME}" > "${RESULTS_DIR}/${case}.locust.log" 2>&1
  )
  locust_result "${RESULTS_DIR}/${case}.locust.log"
}

main() {
  parse_args "$@"
  validate_environment
  [[ ${START} -eq 1 ]] && compose_up
  check_profile
  wait_ready

  local case jmeter locust summary="${RESULTS_DIR}/summary.txt" mismatches=0
  declare -A JMETER_RESULTS LOCUST_RESULTS
  for case in "${CASES[@]}"; do
    log "${case}: restarting the app, then running JMeter"
    restart_app
    JMETER_RESULTS[${case}]="$(run_jmeter "${case}")"
    log "${case}: JMeter ${JMETER_RESULTS[${case}]%% *}"

    log "${case}: restarting the app, then running Locust"
    restart_app
    LOCUST_RESULTS[${case}]="$(run_locust "${case}")"
    log "${case}: Locust ${LOCUST_RESULTS[${case}]%% *}"
  done

  # Not piped into tee, a pipeline would count the mismatches in a subshell
  {
    echo
    echo "Profile: ${TX_MODE}    Outputs: ${RESULTS_DIR}"
    printf '%-6s %-7s %-7s %s\n' "TEST" "JMETER" "LOCUST" "SAME"
    for case in "${CASES[@]}"; do
      jmeter="${JMETER_RESULTS[${case}]%% *}"
      locust="${LOCUST_RESULTS[${case}]%% *}"
      if [[ "${jmeter}" == "${locust}" ]]; then
        printf '%-6s %-7s %-7s %s\n' "${case}" "${jmeter}" "${locust}" "yes"
      else
        printf '%-6s %-7s %-7s %s\n' "${case}" "${jmeter}" "${locust}" "NO"
        mismatches=$((mismatches + 1))
      fi
    done
    for case in "${CASES[@]}"; do
      if [[ "${JMETER_RESULTS[${case}]}" != "PASS" || "${LOCUST_RESULTS[${case}]}" != "PASS" ]]; then
        echo
        echo "${case} JMeter: ${JMETER_RESULTS[${case}]}"
        echo "${case} Locust: ${LOCUST_RESULTS[${case}]}"
      fi
    done
  } > "${summary}"
  cat "${summary}"

  if [[ ${mismatches} -gt 0 ]]; then
    echo
    echo "${mismatches} test(s) where JMeter and Locust disagree."
    exit 1
  fi
}

main "$@"
