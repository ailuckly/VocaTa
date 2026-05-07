#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_DIR="${APP_DIR:-$(cd "$REPO_DIR/.." && pwd)}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env}"
CHARACTER_LIST_RESPONSE="$(mktemp)"

cleanup() {
  rm -f "$CHARACTER_LIST_RESPONSE"
}

trap cleanup EXIT

cd "$REPO_DIR"

echo "==> staging compose status"
docker compose --env-file "$ENV_FILE" ps

echo "==> staging backend health"
curl -fsS -o /dev/null http://127.0.0.1:9009/api/health
echo "staging backend health: ok"

echo "==> staging character list smoke"
curl -fsS -o "$CHARACTER_LIST_RESPONSE" "http://127.0.0.1:9009/api/open/character/list?pageNum=1&pageSize=2"
if ! grep -q '"code":200' "$CHARACTER_LIST_RESPONSE"; then
  echo "staging character list smoke: unexpected response"
  head -c 500 "$CHARACTER_LIST_RESPONSE"
  echo
  exit 1
fi
echo "staging character list smoke: ok"
