#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APPLICATIONS_ROOT="$REPO_ROOT/applications"
SPECS_ROOT="$SCRIPT_DIR/spock"

CYAN="\033[36m"; GREEN="\033[32m"; RED="\033[31m"; YELLOW="\033[33m"; BOLD="\033[1m"; RESET="\033[0m"

step() { echo -e "${CYAN}▶${RESET} $*"; }
ok()   { echo -e "${GREEN}✔${RESET} $*"; }
fail() { echo -e "${RED}✘${RESET} $*"; }
warn() { echo -e "${YELLOW}!${RESET} $*"; }

bash "$SCRIPT_DIR/generate.sh" || exit 1

echo
echo -e "${BOLD}Running Spock tests${RESET}"
echo

test_failures=()
test_count=0
for specs_dir in "$SPECS_ROOT"/*/; do
    [ -d "$specs_dir" ] || continue
    project=$(basename "$specs_dir")
    app_dir="$APPLICATIONS_ROOT/$project"

    if [ ! -d "$app_dir" ]; then
        warn "$project — application not found, skipping"
        continue
    fi

    groovy_count=$(find "$app_dir/src/test" -name "*.groovy" 2>/dev/null | wc -l | tr -d ' ')
    if [ "$groovy_count" -eq 0 ]; then
        warn "$project — no test files, skipping"
        continue
    fi

    test_count=$((test_count + 1))

    step "$project — mvn test ($groovy_count specs)"
    if (cd "$app_dir" && mvn -q -B clean test > /tmp/nebula-test-$project.log 2>&1); then
        ok "$project — tests passed"
        continue
    fi
    if grep -qE "Failed to delete|error while writing|NoSuchFileException.*target/classes" "/tmp/nebula-test-$project.log"; then
        warn "$project — transient FS race, retrying after rm -rf target"
        rm -rf "$app_dir/target"
        if (cd "$app_dir" && mvn -q -B clean test > /tmp/nebula-test-$project.log 2>&1); then
            ok "$project — tests passed (on retry)"
            continue
        fi
    fi
    fail "$project — tests failed (see /tmp/nebula-test-$project.log)"
    test_failures+=("$project")
done

echo
if [ ${#test_failures[@]} -eq 0 ]; then
    echo -e "${GREEN}${BOLD}All $test_count test suites passed${RESET}"
    exit 0
else
    echo -e "${RED}${BOLD}${#test_failures[@]} of $test_count test suites failed:${RESET}"
    for f in "${test_failures[@]}"; do echo "  - $f"; done
    exit 1
fi
