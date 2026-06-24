#!/usr/bin/env bash

set -u

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
QUIZZES_DIR="${PROJECT_ROOT}/applications/quizzes"
TEST_DIR="${QUIZZES_DIR}/jmeter/tournament/thesis-cases"

JMETER_BIN="${JMETER_BIN:-/opt/apache-jmeter-5.6.3/bin/jmeter}"
SERVER="localhost"
PORT="8080"
PROTOCOL="http"
REPETITIONS=5
TX_MODE="sagas"
WAIT_TIMEOUT_SECONDS=120
STARTUP_SETTLE_SECONDS=15
COMPOSE_LOG_TAIL=2000
RESULTS_DIR=""
TEST_INPUT=""

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

usage() {
  cat <<EOF
Usage: $0 --test TEST_FILE [options]

Required:
  --test FILE                JMeter test file (basename or absolute path)

Options:
  --repetitions N            Number of runs per deployment (default: ${REPETITIONS})
  --tx-mode MODE             Transaction mode: sagas or tcc (default: ${TX_MODE})
  --jmeter-bin PATH          JMeter binary path (default: ${JMETER_BIN})
  --server HOST              Target host for JMeter variables (default: ${SERVER})
  --port PORT                Target port for JMeter variables (default: ${PORT})
  --protocol PROTOCOL        http or https (default: ${PROTOCOL})
  --results-dir PATH         Output directory (default: generated timestamp under jmeter-results/)
  --wait-timeout SECONDS     Startup wait timeout in seconds (default: ${WAIT_TIMEOUT_SECONDS})
  --startup-settle SECONDS   Extra delay after health check before JMeter (default: ${STARTUP_SETTLE_SECONDS})
  --compose-log-tail N       Number of docker compose log lines to save per run (default: ${COMPOSE_LOG_TAIL})
  -h, --help                 Show this help

Example:
  $0 --test concurrentAddparticipant.jmx --repetitions 10
  $0 --test 01-concurrentAddparticipant.jmx --tx-mode tcc
EOF
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

ensure_prerequisites() {
  [[ -d "${QUIZZES_DIR}" ]] || fail "Missing quizzes directory at ${QUIZZES_DIR}"
  [[ -d "${TEST_DIR}" ]] || fail "Missing JMeter test directory at ${TEST_DIR}"
  [[ -x "${JMETER_BIN}" ]] || fail "JMeter binary not executable: ${JMETER_BIN}"
  command -v docker >/dev/null 2>&1 || fail "docker is required"
  command -v curl >/dev/null 2>&1 || fail "curl is required"
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --test)
        TEST_INPUT="$2"
        shift 2
        ;;
      --repetitions)
        REPETITIONS="$2"
        shift 2
        ;;
      --tx-mode)
        TX_MODE="$2"
        shift 2
        ;;
      --jmeter-bin)
        JMETER_BIN="$2"
        shift 2
        ;;
      --server)
        SERVER="$2"
        shift 2
        ;;
      --port)
        PORT="$2"
        shift 2
        ;;
      --protocol)
        PROTOCOL="$2"
        shift 2
        ;;
      --results-dir)
        RESULTS_DIR="$2"
        shift 2
        ;;
      --wait-timeout)
        WAIT_TIMEOUT_SECONDS="$2"
        shift 2
        ;;
      --startup-settle)
        STARTUP_SETTLE_SECONDS="$2"
        shift 2
        ;;
      --compose-log-tail)
        COMPOSE_LOG_TAIL="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        fail "Unknown option: $1"
        ;;
    esac
  done

  [[ -n "${TEST_INPUT}" ]] || fail "--test is required"
  [[ "${REPETITIONS}" =~ ^[0-9]+$ ]] || fail "--repetitions must be an integer"
  [[ "${REPETITIONS}" -gt 0 ]] || fail "--repetitions must be > 0"
  [[ "${WAIT_TIMEOUT_SECONDS}" =~ ^[0-9]+$ ]] || fail "--wait-timeout must be an integer"
  [[ "${STARTUP_SETTLE_SECONDS}" =~ ^[0-9]+$ ]] || fail "--startup-settle must be an integer"
  [[ "${COMPOSE_LOG_TAIL}" =~ ^[0-9]+$ ]] || fail "--compose-log-tail must be an integer"
  [[ "${COMPOSE_LOG_TAIL}" -gt 0 ]] || fail "--compose-log-tail must be > 0"

  TX_MODE="${TX_MODE,,}"
  [[ "${TX_MODE}" == "sagas" || "${TX_MODE}" == "tcc" ]] || fail "--tx-mode must be one of: sagas, tcc"

  local results_prefix="${TX_MODE}"
  if [[ "${TX_MODE}" == "tcc" ]]; then
    results_prefix="tcc"
  elif [[ "${TX_MODE}" == "sagas" ]]; then
    results_prefix="sagas"
  fi

  if [[ -z "${RESULTS_DIR}" ]]; then
    RESULTS_DIR="${PROJECT_ROOT}/jmeter-results/${results_prefix}-benchmark_$(date +%Y%m%d_%H%M%S)"
  fi
}

resolve_test_file() {
  if [[ -f "${TEST_INPUT}" ]]; then
    TEST_FILE="${TEST_INPUT}"
  else
    TEST_FILE="${TEST_DIR}/${TEST_INPUT}"
  fi

  [[ -f "${TEST_FILE}" ]] || fail "JMeter test file not found: ${TEST_INPUT}"

  TEST_BASENAME="$(basename "${TEST_FILE}")"
  if [[ "${TX_MODE}" == "sagas" && "${TEST_BASENAME}" =~ ^[0-9] ]]; then
    fail "${TEST_BASENAME} starts with a number and is reserved for TCC tests. Use --tx-mode tcc or choose a Sagas test file."
  fi
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

nearest_rank_percentile() {
  local input_file="$1"
  local percentile="$2"
  local n
  n=$(wc -l < "${input_file}" | tr -d ' ')

  if [[ "${n}" -eq 0 ]]; then
    echo "0"
    return 0
  fi

  local rank
  rank=$(awk -v n="${n}" -v p="${percentile}" 'BEGIN { r = int((p*n + 99) / 100); if (r < 1) r = 1; if (r > n) r = n; print r }')
  awk -v rank="${rank}" 'NR==rank { print $1; exit }' "${input_file}"
}

parse_jtl_metrics() {
  local jtl_file="$1"
  local output_file="$2"

  if [[ ! -f "${jtl_file}" ]]; then
    cat <<EOF > "${output_file}"
total_samples=0
failed_samples=0
success_rate=0.00
mean_ms=0
median_ms=0
p95_ms=0
p99_ms=0
duration_s=0.000
throughput_rps=0.000
EOF
    return
  fi

  local tmp_elapsed
  tmp_elapsed="$(mktemp)"
  tail -n +2 "${jtl_file}" | awk -F',' 'NF > 2 { print $2 }' | grep -E '^[0-9]+$' | sort -n > "${tmp_elapsed}" || true

  local total_samples failed_samples
  total_samples=$(tail -n +2 "${jtl_file}" | awk 'END { print NR + 0 }')
  failed_samples=$(tail -n +2 "${jtl_file}" | awk -F',' 'tolower($8)=="false" { c++ } END { print c + 0 }')

  local success_rate
  success_rate=$(awk -v total="${total_samples}" -v failed="${failed_samples}" 'BEGIN { if (total == 0) { printf "0.00" } else { printf "%.2f", ((total-failed)*100)/total } }')

  local mean_ms median_ms p95_ms p99_ms
  mean_ms=$(awk '{ s+=$1; n++ } END { if (n==0) print 0; else printf "%.2f", s/n }' "${tmp_elapsed}")
  median_ms=$(nearest_rank_percentile "${tmp_elapsed}" 50)
  p95_ms=$(nearest_rank_percentile "${tmp_elapsed}" 95)
  p99_ms=$(nearest_rank_percentile "${tmp_elapsed}" 99)

  local duration_s throughput_rps
  duration_s=$(tail -n +2 "${jtl_file}" | awk -F',' '
    BEGIN { min=0; max=0 }
    {
      ts = $1 + 0;
      el = $2 + 0;
      end = ts + el;
      if (NR == 1 || ts < min) min = ts;
      if (NR == 1 || end > max) max = end;
    }
    END {
      if (NR == 0) {
        printf "0.000";
      } else {
        d = (max - min) / 1000.0;
        if (d < 0) d = 0;
        printf "%.3f", d;
      }
    }')

  throughput_rps=$(awk -v total="${total_samples}" -v d="${duration_s}" 'BEGIN { if (d <= 0) printf "0.000"; else printf "%.3f", total/d }')

  cat <<EOF > "${output_file}"
total_samples=${total_samples}
failed_samples=${failed_samples}
success_rate=${success_rate}
mean_ms=${mean_ms}
median_ms=${median_ms}
p95_ms=${p95_ms}
p99_ms=${p99_ms}
duration_s=${duration_s}
throughput_rps=${throughput_rps}
EOF

  rm -f "${tmp_elapsed}"
}

median_from_column() {
  local csv_file="$1"
  local deployment="$2"
  local column="$3"
  local tmp
  tmp="$(mktemp)"

  awk -F',' -v dep="${deployment}" -v col="${column}" '$1==dep { print $col }' "${csv_file}" | sort -n > "${tmp}"
  local value
  value=$(nearest_rank_percentile "${tmp}" 50)
  rm -f "${tmp}"
  echo "${value:-0}"
}

aggregate_deployment() {
  local deployment="$1"
  local runs_csv="$2"
  local summary_csv="$3"

  local runs pass_runs total_samples failed_samples
  runs=$(awk -F',' -v dep="${deployment}" '$1==dep { c++ } END { print c + 0 }' "${runs_csv}")
  pass_runs=$(awk -F',' -v dep="${deployment}" '$1==dep { s+=$4 } END { print s + 0 }' "${runs_csv}")
  total_samples=$(awk -F',' -v dep="${deployment}" '$1==dep { s+=$5 } END { print s + 0 }' "${runs_csv}")
  failed_samples=$(awk -F',' -v dep="${deployment}" '$1==dep { s+=$6 } END { print s + 0 }' "${runs_csv}")

  local run_pass_rate sample_success_rate
  run_pass_rate=$(awk -v p="${pass_runs}" -v r="${runs}" 'BEGIN { if (r==0) printf "0.00"; else printf "%.2f", (p*100.0)/r }')
  sample_success_rate=$(awk -v t="${total_samples}" -v f="${failed_samples}" 'BEGIN { if (t==0) printf "0.00"; else printf "%.2f", ((t-f)*100.0)/t }')

  local median_latency_ms p95_ms p99_ms median_duration_s median_throughput_rps
  median_latency_ms=$(median_from_column "${runs_csv}" "${deployment}" 9)
  p95_ms=$(median_from_column "${runs_csv}" "${deployment}" 10)
  p99_ms=$(median_from_column "${runs_csv}" "${deployment}" 11)
  median_duration_s=$(median_from_column "${runs_csv}" "${deployment}" 12)
  median_throughput_rps=$(median_from_column "${runs_csv}" "${deployment}" 13)

  echo "${deployment},${runs},${pass_runs},${run_pass_rate},${sample_success_rate},${median_latency_ms},${p95_ms},${p99_ms},${median_duration_s},${median_throughput_rps}" >> "${summary_csv}"
}

write_markdown_summary() {
  local summary_csv="$1"
  local markdown_file="$2"

  {
    echo "# ${TX_MODE^^} Benchmark Summary"
    echo ""
    echo "- Test file: ${TEST_FILE}"
    echo "- Transaction mode: ${TX_MODE}"
    echo "- Repetitions per deployment: ${REPETITIONS}"
    echo "- Target: ${PROTOCOL}://${SERVER}:${PORT}"
    echo "- Generated at: $(date)"
    echo ""
    echo "| Deployment | Runs | Run Pass Rate (%) | Sample Success Rate (%) | Median Latency (ms) | p95 (ms) | p99 (ms) | Median Scenario Duration (s) | Median Throughput (req/s) |"
    echo "|---|---:|---:|---:|---:|---:|---:|---:|---:|"
    tail -n +2 "${summary_csv}" | awk -F',' '{ printf "| %s | %s | %s | %s | %s | %s | %s | %s | %s |\n", $1, $2, $4, $5, $6, $7, $8, $9, $10 }'
    echo ""
    echo "## Deployment Commands (README-aligned, ${TX_MODE^^})"
    echo ""
    echo "| Deployment | Command |"
    echo "|---|---|"
    echo "| centralized-local | TX_MODE=${TX_MODE} docker compose up quizzes-local -d --wait |"
    echo "| centralized-stream | TX_MODE=${TX_MODE} COMM_LAYER=stream docker compose up quizzes-remote version-service -d --wait |"
    echo "| centralized-grpc | TX_MODE=${TX_MODE} COMM_LAYER=grpc docker compose up quizzes-remote version-service -d --wait |"
    echo "| distributed-stream | TX_MODE=${TX_MODE} COMM_LAYER=stream docker compose up gateway version-service answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d --wait |"
    echo "| distributed-grpc | TX_MODE=${TX_MODE} COMM_LAYER=grpc docker compose up gateway version-service answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d --wait |"
    echo "| centralized-stream-distributed-version | TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=stream docker compose up quizzes-remote -d --wait |"
    echo "| centralized-grpc-distributed-version | TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=grpc docker compose up quizzes-remote -d --wait |"
    echo "| distributed-stream-distributed-version | TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=stream docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d --wait |"
    echo "| distributed-grpc-distributed-version | TX_MODE=${TX_MODE} VERSION_MODE=distributed-version COMM_LAYER=grpc docker compose up gateway answer-service course-service execution-service question-service quiz-service topic-service tournament-service user-service -d --wait |"
  } > "${markdown_file}"
}

main() {
  parse_args "$@"
  configure_deployment_commands
  ensure_prerequisites
  resolve_test_file

  mkdir -p "${RESULTS_DIR}"
  RUNS_CSV="${RESULTS_DIR}/runs.csv"
  SUMMARY_CSV="${RESULTS_DIR}/summary.csv"
  SUMMARY_MD="${RESULTS_DIR}/thesis-table.md"

  cat > "${RUNS_CSV}" <<EOF
deployment,run,jmeter_exit,run_pass,total_samples,failed_samples,success_rate,mean_ms,median_ms,p95_ms,p99_ms,duration_s,throughput_rps,jtl_file,log_file,compose_logs_file
EOF

  cat > "${SUMMARY_CSV}" <<EOF
deployment,runs,pass_runs,run_pass_rate,sample_success_rate,median_latency_ms,p95_ms,p99_ms,median_duration_s,median_throughput_rps
EOF

  log "Results directory: ${RESULTS_DIR}"
  log "Selected ${TX_MODE^^} test: ${TEST_FILE}"

  for i in "${!DEPLOYMENT_NAMES[@]}"; do
    deployment="${DEPLOYMENT_NAMES[$i]}"
    command="${DEPLOYMENT_COMMANDS[$i]}"
    log "Starting benchmark for deployment: ${deployment}"
    debug "[${deployment}] Compose command: ${command}"

    for run in $(seq 1 "${REPETITIONS}"); do
      run_prefix="${deployment}__run${run}"
      jtl_file="${RESULTS_DIR}/${run_prefix}.jtl"
      log_file="${RESULTS_DIR}/${run_prefix}.log"
      compose_logs_file="${RESULTS_DIR}/${run_prefix}.compose.log"
      metric_file="${RESULTS_DIR}/${run_prefix}.metrics"

      log "[${deployment}] Run ${run}/${REPETITIONS}: restarting deployment"
      compose_down

      if ! start_deployment "${command}"; then
        log "[${deployment}] Run ${run}: deployment startup failed"
        debug "[${deployment}] Run ${run}: inspect compose services with docker compose ps"
        capture_compose_logs "${compose_logs_file}"
        debug "[${deployment}] Run ${run}: captured compose logs at ${compose_logs_file}"
        echo "${deployment},${run},1,0,0,0,0.00,0,0,0,0,0.000,0.000,${jtl_file},${log_file},${compose_logs_file}" >> "${RUNS_CSV}"
        continue
      fi

      if ! wait_for_http_ready; then
        log "[${deployment}] Run ${run}: health check timeout"
        (
          cd "${QUIZZES_DIR}" || exit 1
          docker compose ps
        ) > "${RESULTS_DIR}/${run_prefix}.compose-ps.txt" 2>&1 || true
        capture_compose_logs "${compose_logs_file}"
        debug "[${deployment}] Run ${run}: captured compose logs at ${compose_logs_file}"
        echo "${deployment},${run},1,0,0,0,0.00,0,0,0,0,0.000,0.000,${jtl_file},${log_file},${compose_logs_file}" >> "${RUNS_CSV}"
        continue
      fi

      # Give Eureka some extra time to converge service registrations before load starts.
      if [[ "${STARTUP_SETTLE_SECONDS}" -gt 0 ]]; then
        log "[${deployment}] Run ${run}: waiting ${STARTUP_SETTLE_SECONDS}s for service discovery convergence"
        sleep "${STARTUP_SETTLE_SECONDS}"
      fi

      log "[${deployment}] Run ${run}: executing JMeter"
      debug "[${deployment}] Run ${run}: JMeter output=${RESULTS_DIR}/${run_prefix}.out, jtl=${jtl_file}, log=${log_file}"
      jmeter_exit=0
      "${JMETER_BIN}" -n \
        -t "${TEST_FILE}" \
        -l "${jtl_file}" \
        -j "${log_file}" \
        -Jserver="${SERVER}" \
        -Jport="${PORT}" \
        -Jprotocol="${PROTOCOL}" \
        > "${RESULTS_DIR}/${run_prefix}.out" 2>&1 || jmeter_exit=$?

      parse_jtl_metrics "${jtl_file}" "${metric_file}"
      # shellcheck disable=SC1090
      source "${metric_file}"

      run_pass=0
      if [[ "${jmeter_exit}" -eq 0 && "${failed_samples}" -eq 0 ]]; then
        run_pass=1
      fi

      log "[${deployment}] Run ${run}: JMeter exit=${jmeter_exit}, run_pass=${run_pass}, success_rate=${success_rate}% (${total_samples}-${failed_samples}/${total_samples}), mean=${mean_ms}ms, p95=${p95_ms}ms, throughput=${throughput_rps} req/s"
      if [[ "${jmeter_exit}" -ne 0 || "${failed_samples}" -gt 0 ]]; then
        debug "[${deployment}] Run ${run}: failure context from ${RESULTS_DIR}/${run_prefix}.out"
        tail -n 20 "${RESULTS_DIR}/${run_prefix}.out" | sed 's/^/[JMETER] /'
      fi

      capture_compose_logs "${compose_logs_file}"
      debug "[${deployment}] Run ${run}: captured compose logs at ${compose_logs_file}"
      echo "${deployment},${run},${jmeter_exit},${run_pass},${total_samples},${failed_samples},${success_rate},${mean_ms},${median_ms},${p95_ms},${p99_ms},${duration_s},${throughput_rps},${jtl_file},${log_file},${compose_logs_file}" >> "${RUNS_CSV}"
    done

    aggregate_deployment "${deployment}" "${RUNS_CSV}" "${SUMMARY_CSV}"
    dep_sample_success_rate=$(awk -F',' -v dep="${deployment}" '$1==dep { sr=$5 } END { if (sr=="") sr="0.00"; print sr }' "${SUMMARY_CSV}")
    dep_run_pass_rate=$(awk -F',' -v dep="${deployment}" '$1==dep { rp=$4 } END { if (rp=="") rp="0.00"; print rp }' "${SUMMARY_CSV}")
    log "[${deployment}] aggregate: run_pass_rate=${dep_run_pass_rate}%, sample_success_rate=${dep_sample_success_rate}%"
  done

  compose_down
  write_markdown_summary "${SUMMARY_CSV}" "${SUMMARY_MD}"

  log "Benchmark complete"
  log "Per-run data: ${RUNS_CSV}"
  log "Summary CSV: ${SUMMARY_CSV}"
  log "Thesis table: ${SUMMARY_MD}"
}

main "$@"
