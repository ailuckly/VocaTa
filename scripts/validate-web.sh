#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_DIR/vocata-web"

echo "==> web node version"
"$SCRIPT_DIR/check-node-version.sh"

echo "==> web lint"
npm run lint

echo "==> web type-check"
npm run type-check

echo "==> web test"
npm run test

echo "==> web build"
npm run build
