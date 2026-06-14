#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_DIR"

echo "==> docs and metadata validation"
"$SCRIPT_DIR/validate-docs.sh"

echo "==> backend validation"
"$SCRIPT_DIR/validate-backend.sh"

echo "==> web validation"
"$SCRIPT_DIR/validate-web.sh"

echo "==> admin validation"
"$SCRIPT_DIR/validate-admin.sh"
