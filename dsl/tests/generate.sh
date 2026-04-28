#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
NEBULA_DIR="$REPO_ROOT/dsl/nebula"
ABSTRACTIONS_ROOT="$REPO_ROOT/dsl/abstractions"
APPLICATIONS_ROOT="$REPO_ROOT/applications"
SPECS_ROOT="$SCRIPT_DIR/spock"

CYAN="\033[36m"; GREEN="\033[32m"; RED="\033[31m"; YELLOW="\033[33m"; BOLD="\033[1m"; RESET="\033[0m"

step() { echo -e "${CYAN}▶${RESET} $*"; }
ok()   { echo -e "${GREEN}✔${RESET} $*"; }
fail() { echo -e "${RED}✘${RESET} $*"; }
warn() { echo -e "${YELLOW}!${RESET} $*"; }

read_base_package() {
    local config="$1/nebula.config.json"
    if [ ! -f "$config" ]; then
        echo "pt.ulisboa.tecnico.socialsoftware"
        return
    fi
    local pkg
    pkg=$(grep -o '"basePackage"[[:space:]]*:[[:space:]]*"[^"]*"' "$config" | sed 's/.*"basePackage"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/' || true)
    echo "${pkg:-pt.ulisboa.tecnico.socialsoftware}"
}

step "building nebula DSL"
(cd "$NEBULA_DIR" && npm run build > /dev/null 2>&1)
ok "build succeeded"

failures=()
count=0
for abs_dir in "$ABSTRACTIONS_ROOT"/*/; do
    [ -d "$abs_dir" ] || continue
    name=$(basename "$abs_dir")
    count=$((count + 1))

    step "$name — generating"
    if ! (cd "$NEBULA_DIR" && ./bin/cli.js generate "$abs_dir" -o "$APPLICATIONS_ROOT/$name" > /tmp/nebula-gen-$name.log 2>&1); then
        fail "$name — generation failed (see /tmp/nebula-gen-$name.log)"
        failures+=("$name")
        continue
    fi
    ok "$name"
done

echo
if [ ${#failures[@]} -gt 0 ]; then
    echo -e "${RED}${BOLD}${#failures[@]} of $count projects failed to generate${RESET}"
    for f in "${failures[@]}"; do echo "  - $f"; done
    exit 1
fi
echo -e "${GREEN}${BOLD}all $count projects generated${RESET}"

echo
step "copying test specs into applications"
specs_count=0
for specs_dir in "$SPECS_ROOT"/*/; do
    [ -d "$specs_dir" ] || continue
    project=$(basename "$specs_dir")
    app_dir="$APPLICATIONS_ROOT/$project"
    abs_dir="$ABSTRACTIONS_ROOT/$project"

    if [ ! -d "$app_dir" ]; then
        warn "$project — application not found, skipping specs"
        continue
    fi

    base_pkg=$(read_base_package "$abs_dir")
    java_project=$(grep -o '<artifactId>[^<]*</artifactId>' "$app_dir/pom.xml" 2>/dev/null | sed 's/<[^>]*>//g' | grep -v '^spring-boot-starter-parent$' | head -1)
    [ -z "$java_project" ] && java_project="$project"

    pkg_path="${base_pkg//.//}/$java_project"
    groovy_dest="$app_dir/src/test/groovy/$pkg_path"
    resources_dest="$app_dir/src/test/resources"

    mkdir -p "$groovy_dest" "$resources_dest"
    rm -f "$groovy_dest"/*.groovy 2>/dev/null || true

    groovy_count=0
    for f in "$specs_dir"/*.groovy; do
        [ -e "$f" ] || break
        cp "$f" "$groovy_dest/"
        groovy_count=$((groovy_count + 1))
    done
    for f in "$specs_dir"/*.properties "$specs_dir"/*.yml "$specs_dir"/*.yaml; do
        [ -e "$f" ] || continue
        cp "$f" "$resources_dest/"
    done

    if [ $groovy_count -gt 0 ]; then
        ok "$project — $groovy_count spec(s) copied"
        specs_count=$((specs_count + 1))
    fi
done

echo
echo -e "${GREEN}${BOLD}Generation complete: $count projects generated, $specs_count test suites copied${RESET}"
