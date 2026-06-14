#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WARN_KB="${WARN_KB:-500}"
OUTPUT_JSON=false

cd "$REPO_DIR"

usage() {
  cat <<'EOF'
Usage: ./scripts/report-frontend-assets.sh [--json] [web|admin|all]

Report production build asset sizes from existing dist directories.

Options:
  --json   Print a machine-readable JSON summary.
  -h, --help
           Show this help.

Environment:
  WARN_KB  Warn when an asset is larger than this size in KiB. Defaults to 500.
EOF
}

target="all"
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
    web|admin|all)
      target="$1"
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

case "$target" in
  web|admin|all)
    ;;
  *)
    echo "Unknown target: $target" >&2
    usage >&2
    exit 2
    ;;
esac

report_module() {
  local name="$1"
  local dist_dir="$2"

  echo "==> $name production assets"

  if [[ ! -d "$dist_dir" ]]; then
    echo "No dist directory found at $dist_dir"
    echo "Run ./scripts/validate-$name.sh first to generate a production build."
    return
  fi

  DIST_DIR="$dist_dir" WARN_KB="$WARN_KB" python3 - <<'PY'
import os
from pathlib import Path

dist_dir = Path(os.environ["DIST_DIR"])
warn_kb = int(os.environ["WARN_KB"])
assets = []

for path in sorted(dist_dir.rglob("*")):
    if path.is_file():
        size_bytes = path.stat().st_size
        assets.append((size_bytes, path.relative_to(dist_dir)))

if not assets:
    print("No files found in dist directory.")
    raise SystemExit(0)

total_bytes = sum(size for size, _path in assets)
large_assets = [(size, path) for size, path in assets if size / 1024 >= warn_kb]

print(f"Total files: {len(assets)}")
print(f"Total size: {total_bytes / 1024:.2f} KiB")
print(f"Warn threshold: {warn_kb} KiB")
print()
print("Largest assets:")
for size, path in sorted(assets, reverse=True)[:20]:
    marker = "WARN" if size / 1024 >= warn_kb else "    "
    print(f"{marker} {size / 1024:10.2f} KiB  {path}")

if large_assets:
    print()
    print(f"Assets over threshold: {len(large_assets)}")
else:
    print()
    print("Assets over threshold: 0")
PY
}

json_report() {
  REPORT_TARGET="$target" WARN_KB="$WARN_KB" python3 - <<'PY'
import json
import os
from pathlib import Path

target = os.environ["REPORT_TARGET"]
warn_kb = int(os.environ["WARN_KB"])
module_specs = []

if target in {"web", "all"}:
    module_specs.append(("web", Path("vocata-web/dist")))
if target in {"admin", "all"}:
    module_specs.append(("admin", Path("vocata-admin/dist")))

modules = []
for name, dist_dir in module_specs:
    module = {
        "name": name,
        "dist_dir": str(dist_dir),
        "exists": dist_dir.is_dir(),
        "file_count": 0,
        "total_size_kib": 0.0,
        "warn_threshold_kib": warn_kb,
        "assets_over_threshold": 0,
        "largest_assets": [],
    }

    if dist_dir.is_dir():
        assets = []
        for path in sorted(dist_dir.rglob("*")):
            if path.is_file():
                size_bytes = path.stat().st_size
                assets.append((size_bytes, path.relative_to(dist_dir)))

        total_bytes = sum(size for size, _path in assets)
        module["file_count"] = len(assets)
        module["total_size_kib"] = round(total_bytes / 1024, 2)
        module["assets_over_threshold"] = sum(1 for size, _path in assets if size / 1024 >= warn_kb)
        module["largest_assets"] = [
            {
                "path": str(path),
                "size_kib": round(size / 1024, 2),
                "over_threshold": size / 1024 >= warn_kb,
            }
            for size, path in sorted(assets, reverse=True)[:20]
        ]

    modules.append(module)

print(json.dumps(
    {
        "target": target,
        "warn_kb": warn_kb,
        "modules": modules,
    },
    ensure_ascii=False,
    indent=2,
))
PY
}

if $OUTPUT_JSON; then
  json_report
  exit 0
fi

if [[ "$target" == "web" || "$target" == "all" ]]; then
  report_module "web" "vocata-web/dist"
fi

if [[ "$target" == "all" ]]; then
  echo
fi

if [[ "$target" == "admin" || "$target" == "all" ]]; then
  report_module "admin" "vocata-admin/dist"
fi
