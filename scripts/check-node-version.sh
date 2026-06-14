#!/usr/bin/env bash

set -euo pipefail

# Keep this policy aligned with package.json engines.node and scripts/validate-docs.sh.
node <<'NODE'
const [major, minor] = process.versions.node.split('.').map(Number)
const supported = (major === 20 && minor >= 19) || (major === 22 && minor >= 12) || major > 22

if (!supported) {
  console.error(`Node.js ${process.versions.node} detected. This project requires Node.js 20.19+, 22.12+, or 24+.`)
  process.exit(1)
}
NODE
