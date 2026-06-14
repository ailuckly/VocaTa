#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_JSON=false

usage() {
  cat <<'EOF'
Usage: ./scripts/doctor.sh [--json]

Check the local development environment and repository validation entrypoints.

Options:
  --json   Print a machine-readable JSON summary.
  -h, --help
           Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --json)
      OUTPUT_JSON=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

cd "$REPO_DIR"

failures=0
warnings=0
missing_env_keys=""
extra_env_keys=""

if $OUTPUT_JSON; then
  json_tmp_dir="$(mktemp -d)"
  checks_file="$json_tmp_dir/checks.tsv"
  : > "$checks_file"
  trap 'rm -rf "$json_tmp_dir"' EXIT
  exec 3>&1
  exec >/dev/null
else
  checks_file=""
fi

record_check() {
  local status="$1"
  local message="$2"
  if $OUTPUT_JSON; then
    printf '%s\t%s\n' "$status" "$message" >> "$checks_file"
  fi
}

ok() {
  record_check "ok" "$1"
  printf 'OK   %s\n' "$1"
}

warn() {
  warnings=$((warnings + 1))
  record_check "warn" "$1"
  printf 'WARN %s\n' "$1"
}

fail() {
  failures=$((failures + 1))
  record_check "fail" "$1"
  printf 'FAIL %s\n' "$1"
}

have_command() {
  command -v "$1" >/dev/null 2>&1
}

version_line() {
  local command_name="$1"
  shift
  if have_command "$command_name"; then
    "$command_name" "$@" 2>/dev/null | head -n 1
  fi
}

parse_major() {
  printf '%s' "$1" | grep -Eo '[0-9]+' | head -n 1
}

env_keys() {
  awk '
    /^[[:space:]]*($|#)/ { next }
    {
      line = $0
      sub(/^[[:space:]]*export[[:space:]]+/, "", line)
      if (line ~ /^[A-Za-z_][A-Za-z0-9_]*[[:space:]]*=/) {
        sub(/[[:space:]]*=.*/, "", line)
        print line
      }
    }
  ' "$1" | sort -u
}

echo "==> VocaTa local development doctor"

echo
echo "==> required commands"
if have_command git; then
  ok "git: $(git --version)"
else
  fail "git is required"
fi

if have_command java; then
  java_version_output="$(java -version 2>&1 | head -n 1)"
  java_major="$(printf '%s' "$java_version_output" | sed -E 's/.*version "([0-9]+).*/\1/')"
  java_baseline="17"
  if [[ -f ".java-version" ]]; then
    java_baseline="$(tr -d '[:space:]' < .java-version)"
  fi

  if [[ "$java_major" == "$java_baseline" ]]; then
    ok "java: $java_version_output"
  else
    warn "java: $java_version_output (project baseline is Java $java_baseline)"
  fi
else
  fail "java is required"
fi

if have_command mvn; then
  ok "maven: $(version_line mvn --version)"
else
  fail "maven is required"
fi

if have_command node; then
  node_version="$(node -p 'process.versions.node')"
  node_supported="$(node - <<'NODE'
const [major, minor] = process.versions.node.split('.').map(Number)
const supported = (major === 20 && minor >= 19) || (major === 22 && minor >= 12) || major > 22
console.log(supported ? 'yes' : 'no')
NODE
)"
  if [[ "$node_supported" == "yes" ]]; then
    ok "node: $node_version"
  else
    fail "node: $node_version (requires 20.19+, 22.12+, or 24+)"
  fi

  if [[ -f ".nvmrc" ]]; then
    nvmrc_version="$(tr -d '[:space:]' < .nvmrc)"
    nvmrc_version="${nvmrc_version#v}"
    if [[ "$node_version" == "$nvmrc_version" ]]; then
      ok ".nvmrc matches current node version"
    else
      warn ".nvmrc recommends Node $nvmrc_version; current node is $node_version"
    fi
  fi
else
  fail "node is required"
fi

if have_command npm; then
  ok "npm: $(npm --version)"
else
  fail "npm is required"
fi

if have_command docker; then
  ok "docker cli: $(version_line docker --version)"
  if docker compose version >/dev/null 2>&1; then
    ok "docker compose: $(docker compose version)"
  else
    warn "docker compose is not available or Docker daemon is not reachable"
  fi
else
  warn "docker is not installed; Docker validation and local PostgreSQL/Redis compose mode will be unavailable"
fi

echo
echo "==> repository files"
for path in \
  ".java-version" \
  ".nvmrc" \
  ".env.example" \
  "docker-compose.yml" \
  "vocata-server/pom.xml" \
  "vocata-web/package.json" \
  "vocata-admin/package.json" \
  "scripts/check.sh" \
  "scripts/pre-pr-check.sh" \
  "scripts/report-frontend-assets.sh" \
  "scripts/validate-docs.sh"; do
  if [[ -f "$path" ]]; then
    ok "$path exists"
  else
    fail "$path is missing"
  fi
done

if [[ -f ".env" ]]; then
  ok ".env exists"
else
  warn ".env is missing; copy .env.example to .env before local startup"
fi

if git check-ignore -q .env; then
  ok ".env is ignored by git"
else
  fail ".env is not ignored by git"
fi

echo
echo "==> sensitive ignore rules"
for path in ".env.prod" "vocata-web/.env.secret" "test-private-key.pem" "test-private-key.key" "database-schema-baseline.sql" ".local/database-schema-baseline.sql" ".claude/settings.local.json"; do
  if git check-ignore -q "$path"; then
    ok "$path is ignored by git"
  else
    fail "$path is not ignored by git"
  fi
done

if git check-ignore -q ".claude/agents/code-reviewer.md"; then
  fail ".claude/agents is ignored by git; shared Claude agents should remain trackable"
else
  ok ".claude/agents is trackable for shared agent definitions"
fi

if git check-ignore -q .env.example; then
  fail ".env.example is ignored by git"
else
  ok ".env.example is allowed as the tracked env template"
fi

if [[ -f ".env.example" && -f ".env" ]]; then
  missing_env_keys="$(
    comm -23 <(env_keys ".env.example") <(env_keys ".env") |
      awk 'BEGIN { first = 1 } { printf "%s%s", first ? "" : ", ", $0; first = 0 }'
  )"
  extra_env_keys="$(
    comm -13 <(env_keys ".env.example") <(env_keys ".env") |
      awk 'BEGIN { first = 1 } { printf "%s%s", first ? "" : ", ", $0; first = 0 }'
  )"

  if [[ -z "$missing_env_keys" && -z "$extra_env_keys" ]]; then
    ok ".env keys match .env.example"
  else
    if [[ -n "$missing_env_keys" ]]; then
      warn ".env missing keys from .env.example: $missing_env_keys"
    fi
    if [[ -n "$extra_env_keys" ]]; then
      warn ".env has keys not in .env.example: $extra_env_keys"
    fi
  fi
fi

echo
echo "==> local dependency folders"
if [[ -d "vocata-web/node_modules" ]]; then
  ok "vocata-web/node_modules exists"
else
  warn "vocata-web/node_modules missing; run npm install in vocata-web"
fi

if [[ -d "vocata-admin/node_modules" ]]; then
  ok "vocata-admin/node_modules exists"
else
  warn "vocata-admin/node_modules missing; run npm install in vocata-admin"
fi

if [[ -d "$HOME/.m2/repository" || -d "/tmp/juhao_m2repo" ]]; then
  ok "Maven repository cache found"
else
  warn "No Maven repository cache found; first backend validation may download dependencies"
fi

echo
echo "==> optional validation entrypoints"
if [[ -x "scripts/check.sh" ]]; then
  ok "scripts/check.sh is executable"
else
  fail "scripts/check.sh is not executable"
fi

if [[ -x "scripts/pre-pr-check.sh" ]]; then
  ok "scripts/pre-pr-check.sh is executable"
else
  fail "scripts/pre-pr-check.sh is not executable"
fi

if [[ -x "scripts/report-frontend-assets.sh" ]]; then
  ok "scripts/report-frontend-assets.sh is executable"
else
  fail "scripts/report-frontend-assets.sh is not executable"
fi

if [[ -x "scripts/validate-docs.sh" ]]; then
  ok "scripts/validate-docs.sh is executable"
else
  fail "scripts/validate-docs.sh is not executable"
fi

echo
echo "==> summary"
echo "Failures: $failures"
echo "Warnings: $warnings"

if $OUTPUT_JSON; then
  exec 1>&3
  export DOCTOR_FAILURES="$failures"
  export DOCTOR_WARNINGS="$warnings"
  export DOCTOR_CHECKS_FILE="$checks_file"
  export DOCTOR_MISSING_ENV_KEYS="$missing_env_keys"
  export DOCTOR_EXTRA_ENV_KEYS="$extra_env_keys"
  python3 - <<'PY'
import json
import os
from pathlib import Path


def split_keys(value):
    if not value:
        return []
    return [item.strip() for item in value.split(",") if item.strip()]


checks = []
checks_file = Path(os.environ["DOCTOR_CHECKS_FILE"])
if checks_file.exists():
    for line in checks_file.read_text(encoding="utf-8").splitlines():
        if not line:
            continue
        status, message = line.split("\t", 1)
        checks.append({"status": status, "message": message})

print(json.dumps(
    {
        "failures": int(os.environ["DOCTOR_FAILURES"]),
        "warnings": int(os.environ["DOCTOR_WARNINGS"]),
        "checks": checks,
        "env_key_drift": {
            "missing": split_keys(os.environ.get("DOCTOR_MISSING_ENV_KEYS", "")),
            "extra": split_keys(os.environ.get("DOCTOR_EXTRA_ENV_KEYS", "")),
        },
    },
    ensure_ascii=False,
    indent=2,
))
PY
fi

if [[ "$failures" -gt 0 ]]; then
  exit 1
fi
