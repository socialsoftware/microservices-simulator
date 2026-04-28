#!/usr/bin/env bash

set -u

MODE="host-postgres"
APPS_FILTER=""
NO_BUILD=0
PARALLEL=1
while [ $# -gt 0 ]; do
    case "$1" in
        --mode) MODE="$2"; shift 2 ;;
        --mode=*) MODE="${1#*=}"; shift ;;
        --apps) APPS_FILTER="$2"; shift 2 ;;
        --apps=*) APPS_FILTER="${1#*=}"; shift ;;
        --no-build) NO_BUILD=1; shift ;;
        --parallel|-j) PARALLEL="$2"; shift 2 ;;
        --parallel=*|-j=*) PARALLEL="${1#*=}"; shift ;;
        -h|--help)
            sed -n '2,30p' "$0" | sed 's|^# \{0,1\}||'
            echo
            echo "Flags:"
            echo "  --mode <host-postgres|host-h2|docker-postgres>  default host-postgres"
            echo "  --apps <name1,name2,...>                        only test the listed apps"
            echo "  --no-build                                      skip regen+package, reuse target/*.jar"
            echo "  --parallel N | -j N                             run up to N apps concurrently (default 1)"
            exit 0 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

if ! [[ "$PARALLEL" =~ ^[0-9]+$ ]] || [ "$PARALLEL" -lt 1 ]; then
    echo "Invalid --parallel value: $PARALLEL (must be positive integer)"; exit 1
fi

apps_filter_match() {
    [ -z "$APPS_FILTER" ] && return 0
    case ",$APPS_FILTER," in *,"$1",*) return 0 ;; esac
    return 1
}

case "$MODE" in
    host-postgres|host-h2|docker-postgres) ;;
    *) echo "Invalid mode: $MODE (expected host-postgres, host-h2, docker-postgres)"; exit 1 ;;
esac

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
NEBULA_CLI="$REPO_ROOT/dsl/nebula/bin/cli.js"
ABSTRACTIONS_DIR="$REPO_ROOT/dsl/abstractions"
APPLICATIONS_DIR="$REPO_ROOT/applications"

PG_USER="postgres"
PG_PASSWORD="postgres"
DOCKER_NETWORK="microservices-simulator_messaging-network"
DOCKER_HOST_PORT_BASE=18000

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }

needs_docker() {
    case "$MODE" in host-postgres|docker-postgres) return 0 ;; *) return 1 ;; esac
}

if needs_docker; then
    if ! docker info > /dev/null 2>&1; then
        red "Docker is not running."; echo
        echo "Start Docker Desktop and run \`docker compose up -d postgres\` first."
        exit 1
    fi
    if ! docker exec postgres true > /dev/null 2>&1; then
        red "Postgres container is not running."; echo
        echo "Run \`docker compose up -d postgres\` from $REPO_ROOT first."
        exit 1
    fi
fi

reset_database() {
    local db=$1
    docker exec postgres psql -U "$PG_USER" -c "DROP DATABASE IF EXISTS $db;" > /dev/null 2>&1
    docker exec postgres psql -U "$PG_USER" -c "CREATE DATABASE $db;" > /dev/null 2>&1
}

no_build_label=""
[ "$NO_BUILD" -eq 1 ] && no_build_label="  --no-build"
parallel_label=""
[ "$PARALLEL" -gt 1 ] && parallel_label="  Parallel: $PARALLEL"
echo "==> Mode: $MODE${APPS_FILTER:+  Apps: $APPS_FILTER}$no_build_label$parallel_label"

RESULTS_DIR="/tmp/nebula-test-results"
rm -rf "$RESULTS_DIR"
mkdir -p "$RESULTS_DIR"

if [ "$NO_BUILD" -eq 0 ]; then
    bash "$SCRIPT_DIR/generate.sh" || exit 1
fi

declare -i pass=0 fail=0
declare -a failures=()

for f in "$SCRIPT_DIR/crud"/*.sh; do
    [ -e "$f" ] || continue
    source "$f"
done

crud_for() {
    local fn="crud_${1//-/_}"
    if declare -f "$fn" > /dev/null; then echo "$fn"; fi
}


ensure_jar() {
    local dir=$1 logfile=$2
    local jar
    jar=$(ls "$dir"/target/*.jar 2>/dev/null | grep -v 'original' | head -1)
    if [ -z "$jar" ] && [ "$NO_BUILD" -eq 0 ]; then
        printf '      \033[90m[mvn package]\033[0m streaming below\n' >&2
        (cd "$dir" && mvn package -DskipTests 2>&1 | tee "$logfile" \
            | grep --line-buffered -E '^\[INFO\] (Building|---|BUILD|Compiling|Tests)|^\[ERROR\]|^\[WARNING\]' \
            | sed 's/^/      | /' >&2) || return 1
        jar=$(ls "$dir"/target/*.jar 2>/dev/null | grep -v 'original' | head -1)
    fi
    [ -n "$jar" ] || return 1
    echo "$jar"
}

boot_host_postgres() {
    local dir=$1 db=$2 port=$3 logfile=$4
    reset_database "$db"
    lsof -ti:"$port" 2>/dev/null | xargs kill -9 2>/dev/null
    local jar
    jar=$(ensure_jar "$dir" "$logfile") || return 1
    java -jar "$jar" \
        --spring.datasource.url=jdbc:postgresql://localhost:5432/$db \
        --spring.datasource.username=$PG_USER \
        --spring.datasource.password=$PG_PASSWORD \
        > "$logfile" 2>&1 &
    echo $!
}

boot_host_h2() {
    local dir=$1 db=$2 port=$3 logfile=$4
    lsof -ti:"$port" 2>/dev/null | xargs kill -9 2>/dev/null
    local jar
    jar=$(ensure_jar "$dir" "$logfile") || return 1
    java -jar "$jar" \
        --spring.datasource.url=jdbc:h2:mem:${db} \
        --spring.datasource.driver-class-name=org.h2.Driver \
        --spring.datasource.username=sa \
        --spring.datasource.password= \
        --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
        > "$logfile" 2>&1 &
    echo $!
}

boot_docker_postgres() {
    local dir=$1 db=$2 port=$3 logfile=$4 name=$5
    reset_database "$db"
    (cd "$dir" && mvn -q package -DskipTests > "$logfile" 2>&1) || return 1
    local jar
    jar=$(ls "$dir"/target/*.jar 2>/dev/null | grep -v 'original' | head -1)
    if [ -z "$jar" ]; then echo "no jar produced" >> "$logfile"; return 1; fi

    docker rm -f "nebula-test-$name" > /dev/null 2>&1
    docker build -q -t "nebula-test-$name" -f - "$dir" >> "$logfile" 2>&1 <<EOF
FROM eclipse-temurin:21-jre
COPY $(basename "$jar") /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF
    [ $? -eq 0 ] || return 1

    docker run -d --name "nebula-test-$name" \
        --network "$DOCKER_NETWORK" \
        -p "$port:$port" \
        -e SPRING_DATASOURCE_URL="jdbc:postgresql://postgres:5432/$db" \
        -e SPRING_DATASOURCE_USERNAME="$PG_USER" \
        -e SPRING_DATASOURCE_PASSWORD="$PG_PASSWORD" \
        "nebula-test-$name" >> "$logfile" 2>&1 || return 1

    echo "nebula-test-$name"
}

wait_for_started() {
    local logfile=$1 marker=$2
    local last_line=""
    for i in $(seq 1 60); do
        sleep 1
        if grep -q "Started .* in .* seconds" "$logfile" 2>/dev/null; then
            printf '      \033[90m| Started\033[0m\n'
            return 0
        fi
        if grep -q "APPLICATION FAILED\|Application run failed" "$logfile" 2>/dev/null; then
            printf '      \033[31m| FAILED\033[0m — last log line:\n'
            tail -5 "$logfile" 2>/dev/null | sed 's/^/      | /'
            return 1
        fi
        if [ $((i % 3)) -eq 0 ]; then
            local current
            current=$(tail -1 "$logfile" 2>/dev/null | cut -c1-100)
            if [ -n "$current" ] && [ "$current" != "$last_line" ]; then
                printf '      \033[90m| [%2ds] %s\033[0m\n' "$i" "$current"
                last_line="$current"
            fi
        fi
    done
    printf '      \033[31m| timed out after 60s\033[0m\n'
    return 1
}

wait_for_container_started() {
    local container=$1
    for _ in $(seq 1 60); do
        sleep 1
        local logs
        logs=$(docker logs "$container" 2>&1)
        if echo "$logs" | grep -q "Started .* in .* seconds"; then return 0; fi
        if echo "$logs" | grep -q "APPLICATION FAILED\|Application run failed"; then return 1; fi
    done
    return 1
}

step() { printf '    \033[90m→\033[0m %s\n' "$1"; }

test_app() {
    local dir=$1
    local name
    name=$(basename "$dir")
    local boot_log="$RESULTS_DIR/boot-$name.log"
    local status_file="$RESULTS_DIR/status-$name"

    echo "  $name"
    local props="$dir/src/main/resources/application.properties"
    if [ ! -f "$props" ]; then
        echo "    $(red ✗) no application.properties"
        echo "fail:no application.properties" > "$status_file"
        return
    fi
    local db port
    db=$(grep "spring.datasource.url" "$props" | sed -E 's|.*/([^?/]+).*|\1|')
    port=$(grep "^server.port=" "$props" | cut -d= -f2)
    if [ -z "$db" ] || [ -z "$port" ]; then
        echo "    $(red ✗) could not parse db or port"
        echo "fail:missing db/port" > "$status_file"
        return
    fi

    rm -f "$boot_log"
    local pid="" container="" started=0
    local t0=$SECONDS

    case "$MODE" in
        host-postgres)
            step "boot (host, Postgres)"
            pid=$(boot_host_postgres "$dir" "$db" "$port" "$boot_log")
            wait_for_started "$boot_log" "Started" && started=1
            ;;
        host-h2)
            step "boot (host, H2)"
            pid=$(boot_host_h2 "$dir" "$db" "$port" "$boot_log")
            wait_for_started "$boot_log" "Started" && started=1
            ;;
        docker-postgres)
            step "package + docker build + run"
            container=$(boot_docker_postgres "$dir" "$db" "$port" "$boot_log" "$name")
            if [ -n "$container" ]; then
                wait_for_container_started "$container" && started=1
                docker logs "$container" > "$boot_log" 2>&1
            fi
            ;;
    esac

    local crud_result=""
    local crud_fn
    crud_fn=$(crud_for "$name")
    if [ "$started" -eq 1 ] && [ -n "$crud_fn" ]; then
        step "CRUD smoke test"
        sleep 1
        local RESP="$RESULTS_DIR/curl-$name.json"
        if crud_msg=$($crud_fn "http://localhost:$port" 2>&1); then
            crud_result="ok"
        else
            crud_result="$crud_msg"
        fi
    fi

    step "shutdown"
    if [ -n "$pid" ]; then
        kill "$pid" 2>/dev/null
        wait "$pid" 2>/dev/null
        lsof -ti:"$port" 2>/dev/null | xargs kill -9 2>/dev/null
    fi
    if [ -n "$container" ]; then
        docker rm -f "$container" > /dev/null 2>&1
        docker rmi -f "$container" > /dev/null 2>&1
    fi

    local elapsed=$((SECONDS - t0))

    if [ "$started" -ne 1 ]; then
        local cause
        cause=$(grep -m1 "Caused by" "$boot_log" 2>/dev/null | head -c 200)
        echo "    $(red ✗) boot failed in ${elapsed}s${cause:+ — $cause}"
        echo "fail:boot failed${cause:+ — $cause}" > "$status_file"
    elif [ -n "$crud_fn" ] && [ "$crud_result" != "ok" ]; then
        echo "    $(red ✗) CRUD failed in ${elapsed}s: $crud_result"
        echo "fail:CRUD failed: $crud_result" > "$status_file"
    elif [ -n "$crud_fn" ]; then
        echo "    $(green ✓) boot + CRUD passed in ${elapsed}s"
        echo "pass" > "$status_file"
    else
        echo "    $(green ✓) boot passed in ${elapsed}s"
        echo "pass" > "$status_file"
    fi
}

collect_target_apps() {
    local apps=()
    for app_dir in "$APPLICATIONS_DIR"/*/; do
        [ -d "$app_dir" ] || continue
        local name
        name=$(basename "$app_dir")
        [ "$name" = "gateway" ] || [ "$name" = "quizzes" ] && continue
        apps_filter_match "$name" || continue
        apps+=("$app_dir")
    done
    printf '%s\n' "${apps[@]}"
}

echo "==> Compiling and booting all applications"

TARGETS=()
while IFS= read -r line; do
    [ -n "$line" ] && TARGETS+=("$line")
done < <(collect_target_apps)

if [ ${#TARGETS[@]} -eq 0 ]; then
    echo "No applications matched the filter."
    exit 0
fi

if [ "$PARALLEL" -eq 1 ]; then
    for app_dir in "${TARGETS[@]}"; do
        test_app "$app_dir"
    done
else
    sem_file=$(mktemp -u "$RESULTS_DIR/sem.XXXXXX")
    mkfifo "$sem_file"
    exec 9<>"$sem_file"
    rm "$sem_file"
    for _ in $(seq 1 "$PARALLEL"); do echo >&9; done

    pids=()
    for app_dir in "${TARGETS[@]}"; do
        read -u 9 _
        name=$(basename "$app_dir")
        out_file="$RESULTS_DIR/out-$name"
        (
            test_app "$app_dir" > "$out_file" 2>&1
            echo >&9
        ) &
        pids+=($!)
        echo "  → launched $name (pid $!)"
    done

    echo "==> Waiting for ${#pids[@]} app(s) to finish..."
    for pid in "${pids[@]}"; do
        wait "$pid" 2>/dev/null || true
    done
    exec 9>&-

    for app_dir in "${TARGETS[@]}"; do
        name=$(basename "$app_dir")
        out_file="$RESULTS_DIR/out-$name"
        if [ -f "$out_file" ]; then
            cat "$out_file"
        fi
    done
fi

declare -i pass=0 fail=0
declare -a failures=()
for app_dir in "${TARGETS[@]}"; do
    name=$(basename "$app_dir")
    status_file="$RESULTS_DIR/status-$name"
    if [ ! -f "$status_file" ]; then
        fail+=1
        failures+=("$name: no status recorded")
        continue
    fi
    status=$(cat "$status_file")
    case "$status" in
        pass) pass+=1 ;;
        fail:*) fail+=1; failures+=("$name: ${status#fail:}") ;;
        *) fail+=1; failures+=("$name: unknown status ($status)") ;;
    esac
done

echo
echo "==> Results: $(green "$pass passed"), $(if [ "$fail" -gt 0 ]; then red "$fail failed"; else echo "0 failed"; fi)"

if [ "$fail" -gt 0 ]; then
    echo
    echo "Failures:"
    for f in "${failures[@]}"; do echo "  - $f"; done
    exit 1
fi
