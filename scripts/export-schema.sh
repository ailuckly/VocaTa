#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

OUTPUT_FILE="${1:-.local/database-schema-baseline.sql}"

cd "$REPO_DIR"

if [[ ! -f ".env" ]]; then
  echo "Missing .env. Copy .env.example to .env first." >&2
  exit 1
fi

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump is required for schema export." >&2
  exit 1
fi

set -a
source .env
set +a

if [[ -z "${DB_HOST:-}" || -z "${DB_PORT:-}" || -z "${DB_NAME:-}" || -z "${DB_USERNAME:-}" ]]; then
  echo "DB_HOST, DB_PORT, DB_NAME, and DB_USERNAME must be set in .env." >&2
  exit 1
fi

if [[ -n "${DB_PASSWORD:-}" ]]; then
  export PGPASSWORD="$DB_PASSWORD"
fi

mkdir -p "$(dirname "$OUTPUT_FILE")"

pg_dump \
  --schema-only \
  --no-owner \
  --no-privileges \
  --dbname="postgresql://${DB_USERNAME}@${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  --file="$OUTPUT_FILE"

if [[ -n "${PGPASSWORD:-}" ]]; then
  unset PGPASSWORD
fi

echo "Schema-only export written to $OUTPUT_FILE"
