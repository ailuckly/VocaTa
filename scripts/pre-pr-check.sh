#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_COMMANDS=false
OUTPUT_JSON=false
BASE_REF="${BASE_REF:-origin/develop}"
CHANGED_FILE_SAMPLE_LIMIT=30

usage() {
  cat <<'EOF'
Usage: ./scripts/pre-pr-check.sh [--run] [--json] [--base <ref>]

Summarize changed areas and print the validation commands expected before a PR.

Options:
  --run         Run the recommended local validation commands.
  --json        Print a machine-readable JSON summary. Cannot be combined with --run.
  --base <ref>  Compare against this git ref when available. Defaults to origin/develop.
  -h, --help    Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run)
      RUN_COMMANDS=true
      shift
      ;;
    --json)
      OUTPUT_JSON=true
      shift
      ;;
    --base)
      if [[ $# -lt 2 ]]; then
        echo "--base requires a ref" >&2
        exit 2
      fi
      BASE_REF="$2"
      shift 2
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

if $OUTPUT_JSON && $RUN_COMMANDS; then
  echo "--json cannot be combined with --run" >&2
  exit 2
fi

cd "$REPO_DIR"

git_names() {
  git -c core.quotePath=false "$@" --name-only
}

git_list_files() {
  git -c core.quotePath=false ls-files "$@"
}

branch="$(git branch --show-current || true)"
if [[ -z "$branch" ]]; then
  branch="(detached HEAD)"
fi

if git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  mapfile -t changed_files < <(
    {
      git_names diff "$BASE_REF"...HEAD
      git_names diff
      git_names diff --cached
      git_list_files --others --exclude-standard
    } | sort -u
  )
else
  mapfile -t changed_files < <(
    {
      git_names diff
      git_names diff --cached
      git_list_files --others --exclude-standard
    } | sort -u
  )
fi

has_docs=false
has_backend=false
has_web=false
has_admin=false
has_docker=false
has_workflows=false
has_env=false
has_scripts=false
has_release=false
has_auth_risk=false
has_database_risk=false
has_ai_streaming_risk=false
has_api_contract_risk=false
has_dependency_risk=false

for file in "${changed_files[@]}"; do
  case "$file" in
    vocata-server/*)
      has_backend=true
      ;;
    vocata-web/*)
      has_web=true
      ;;
    vocata-admin/*)
      has_admin=true
      ;;
  esac

  case "$file" in
    *.md|docs/*|AGENTS.md|CLAUDE.md|CONTRIBUTING.md|CODE_STYLE.md|README.md|SECURITY.md|SUPPORT.md|MAINTAINERS.md|CHANGELOG.md|CODE_OF_CONDUCT.md|LICENSE|.github/ISSUE_TEMPLATE/*|.github/pull_request_template.md|.gitattributes|.editorconfig)
      has_docs=true
      ;;
    scripts/*)
      has_scripts=true
      ;;
    .github/workflows/*)
      has_workflows=true
      ;;
    Dockerfile|docker-compose*.yml|vocata-server/Dockerfile|vocata-web/Dockerfile|vocata-admin/Dockerfile|docs/Docker*|docs/部署*|docs/发布*)
      has_docker=true
      ;;
    .env.example|*.env.example|vocata-server/src/main/resources/application*.yml)
      has_env=true
      ;;
    docs/发布*|docs/版本发布策略.md|.github/workflows/release.yml|.github/workflows/cd-production.yml|.github/workflows/cd-staging.yml|.github/workflows/emergency-rollback.yml)
      has_release=true
      ;;
  esac

  case "$file" in
    vocata-server/src/main/java/com/vocata/auth/*|vocata-server/src/main/java/com/vocata/config/SaTokenConfig.java|vocata-server/src/main/java/com/vocata/common/utils/UserContext.java|vocata-server/src/main/java/com/vocata/admin/controller/*)
      has_auth_risk=true
      ;;
    vocata-server/src/main/java/com/vocata/*/entity/*|vocata-server/src/main/java/com/vocata/*/mapper/*|vocata-server/src/main/resources/mapper/*|vocata-server/src/main/resources/db/*|docs/数据库*|docs/adr/*database*)
      has_database_risk=true
      ;;
    vocata-server/src/main/java/com/vocata/ai/*|vocata-server/src/main/java/com/vocata/voice/*|vocata-server/src/main/java/com/vocata/config/WebSocketConfig.java|docs/AI核心链路测试夹具方案.md|docs/adr/*core-ai*)
      has_ai_streaming_risk=true
      ;;
    vocata-server/src/main/java/com/vocata/*/controller/*|vocata-server/src/main/java/com/vocata/*/dto/*|vocata-web/src/api/*|vocata-admin/src/api/*)
      has_api_contract_risk=true
      ;;
    pom.xml|vocata-server/pom.xml|package.json|package-lock.json|vocata-web/package.json|vocata-web/package-lock.json|vocata-admin/package.json|vocata-admin/package-lock.json)
      has_dependency_risk=true
      ;;
  esac
done

risk_hints=()
add_risk_hint() {
  local hint="$1"
  risk_hints+=("$hint")
}

if $has_auth_risk; then
  add_risk_hint "auth/permission: document Sa-Token route, UserContext, admin/user boundary, and rollback impact."
fi

if $has_database_risk; then
  add_risk_hint "database/schema: confirm whether vocata_* table shape or mapper semantics changed; schema changes need separate approval."
fi

if $has_ai_streaming_risk; then
  add_risk_hint "ai-streaming/provider: review WebSocket streaming, STT -> LLM -> TTS, provider fallback, timeout, and secret-handling impact."
fi

if $has_api_contract_risk; then
  add_risk_hint "api-contract: document route, request/response, pagination, error, and string ID compatibility impact."
fi

if $has_dependency_risk; then
  add_risk_hint "dependency/tooling: explain added, removed, or upgraded dependencies and lockfile changes."
fi

if $has_docker || $has_env; then
  add_risk_hint "docker/env: document host-run vs full Docker configuration impact and .env.example drift."
fi

if $has_workflows; then
  add_risk_hint "ci/workflow: document validation, trigger, secret, permission, and required manual review impact."
fi

if $has_release; then
  add_risk_hint "release: document deployment, release, rollback, and required manual review impact."
fi

commands=()
add_command() {
  local command="$1"
  local existing
  for existing in "${commands[@]}"; do
    if [[ "$existing" == "$command" ]]; then
      return
    fi
  done
  commands+=("$command")
}

add_command "./scripts/doctor.sh"
add_command "./scripts/validate-docs.sh"

if $has_backend || $has_scripts || $has_workflows; then
  add_command "./scripts/validate-backend.sh"
fi

if $has_web || $has_scripts || $has_workflows; then
  add_command "./scripts/validate-web.sh"
fi

if $has_admin || $has_scripts || $has_workflows; then
  add_command "./scripts/validate-admin.sh"
fi

if $has_web || $has_admin || $has_scripts; then
  add_command "./scripts/report-frontend-assets.sh"
fi

if $has_docker || $has_env || $has_release; then
  add_command "./scripts/validate-docker.sh"
fi

if $has_release; then
  add_command "# Review docs/发布检查清单.md and docs/版本发布策略.md manually"
fi

if $OUTPUT_JSON; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "$tmp_dir"' EXIT

  : > "$tmp_dir/changed_files"
  : > "$tmp_dir/commands"
  : > "$tmp_dir/risk_hints"

  if [[ ${#changed_files[@]} -gt 0 ]]; then
    printf '%s\n' "${changed_files[@]}" > "$tmp_dir/changed_files"
  fi
  if [[ ${#commands[@]} -gt 0 ]]; then
    printf '%s\n' "${commands[@]}" > "$tmp_dir/commands"
  fi
  if [[ ${#risk_hints[@]} -gt 0 ]]; then
    printf '%s\n' "${risk_hints[@]}" > "$tmp_dir/risk_hints"
  fi

  export PRE_PR_TMP_DIR="$tmp_dir"
  export PRE_PR_BRANCH="$branch"
  export PRE_PR_BASE_REF="$BASE_REF"
  export PRE_PR_CHANGED_FILE_COUNT="${#changed_files[@]}"
  export PRE_PR_CHANGED_FILE_SAMPLE_LIMIT="$CHANGED_FILE_SAMPLE_LIMIT"
  export PRE_PR_HAS_DOCS="$has_docs"
  export PRE_PR_HAS_BACKEND="$has_backend"
  export PRE_PR_HAS_WEB="$has_web"
  export PRE_PR_HAS_ADMIN="$has_admin"
  export PRE_PR_HAS_SCRIPTS="$has_scripts"
  export PRE_PR_HAS_DOCKER="$has_docker"
  export PRE_PR_HAS_ENV="$has_env"
  export PRE_PR_HAS_WORKFLOWS="$has_workflows"
  export PRE_PR_HAS_RELEASE="$has_release"
  export PRE_PR_HAS_AUTH_RISK="$has_auth_risk"
  export PRE_PR_HAS_DATABASE_RISK="$has_database_risk"
  export PRE_PR_HAS_AI_STREAMING_RISK="$has_ai_streaming_risk"
  export PRE_PR_HAS_API_CONTRACT_RISK="$has_api_contract_risk"
  export PRE_PR_HAS_DEPENDENCY_RISK="$has_dependency_risk"

  python3 - <<'PY'
import json
import os
from pathlib import Path

tmp_dir = Path(os.environ["PRE_PR_TMP_DIR"])


def read_lines(name):
    path = tmp_dir / name
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8")
    if not text:
        return []
    return text.splitlines()


def bool_env(name):
    return os.environ[name] == "true"


changed_files = read_lines("changed_files")
sample_limit = int(os.environ["PRE_PR_CHANGED_FILE_SAMPLE_LIMIT"])

summary = {
    "branch": os.environ["PRE_PR_BRANCH"],
    "base_ref": os.environ["PRE_PR_BASE_REF"],
    "changed_file_count": int(os.environ["PRE_PR_CHANGED_FILE_COUNT"]),
    "areas": {
        "docs_templates_metadata": bool_env("PRE_PR_HAS_DOCS"),
        "backend": bool_env("PRE_PR_HAS_BACKEND"),
        "web": bool_env("PRE_PR_HAS_WEB"),
        "admin": bool_env("PRE_PR_HAS_ADMIN"),
        "scripts": bool_env("PRE_PR_HAS_SCRIPTS"),
        "docker_deploy": bool_env("PRE_PR_HAS_DOCKER"),
        "env_config": bool_env("PRE_PR_HAS_ENV"),
        "workflows": bool_env("PRE_PR_HAS_WORKFLOWS"),
        "release_docs_workflows": bool_env("PRE_PR_HAS_RELEASE"),
    },
    "risk_flags": {
        "auth_permission": bool_env("PRE_PR_HAS_AUTH_RISK"),
        "database_schema": bool_env("PRE_PR_HAS_DATABASE_RISK"),
        "ai_streaming_provider": bool_env("PRE_PR_HAS_AI_STREAMING_RISK"),
        "api_contract": bool_env("PRE_PR_HAS_API_CONTRACT_RISK"),
        "dependency_tooling": bool_env("PRE_PR_HAS_DEPENDENCY_RISK"),
    },
    "recommended_commands": read_lines("commands"),
    "risk_review_hints": read_lines("risk_hints"),
    "changed_file_sample": changed_files[:sample_limit],
    "changed_file_sample_limit": sample_limit,
    "has_more_changed_files": len(changed_files) > sample_limit,
}

print(json.dumps(summary, ensure_ascii=False, indent=2))
PY
  exit 0
fi

cat <<EOF
==> pre-PR validation plan
Branch: $branch
Base ref: $BASE_REF
Changed files: ${#changed_files[@]}

Areas:
  docs/templates/metadata: $has_docs
  backend: $has_backend
  web: $has_web
  admin: $has_admin
  scripts: $has_scripts
  docker/deploy: $has_docker
  env config: $has_env
  workflows: $has_workflows
  release docs/workflows: $has_release
  auth/permission risk: $has_auth_risk
  database/schema risk: $has_database_risk
  ai-streaming/provider risk: $has_ai_streaming_risk
  api-contract risk: $has_api_contract_risk
  dependency/tooling risk: $has_dependency_risk

Recommended commands:
EOF

for command in "${commands[@]}"; do
  echo "  - $command"
done

echo
echo "Risk review hints:"
if [[ ${#risk_hints[@]} -eq 0 ]]; then
  echo "  - none detected from file paths; still review PR scope manually."
else
  for hint in "${risk_hints[@]}"; do
    echo "  - $hint"
  done
fi

if [[ ${#changed_files[@]} -gt 0 ]]; then
  echo
  echo "Changed file sample:"
  printf '  - %s\n' "${changed_files[@]:0:$CHANGED_FILE_SAMPLE_LIMIT}"
  if [[ ${#changed_files[@]} -gt $CHANGED_FILE_SAMPLE_LIMIT ]]; then
    echo "  ... (${#changed_files[@]} total)"
  fi
fi

if ! $RUN_COMMANDS; then
  echo
  echo "Use ./scripts/pre-pr-check.sh --run to execute the recommended commands."
  exit 0
fi

echo
echo "==> running recommended commands"
for command in "${commands[@]}"; do
  if [[ "$command" == \#* ]]; then
    echo "SKIP manual step: ${command#"# "}"
    continue
  fi
  echo "==> $command"
  bash -lc "$command"
done
