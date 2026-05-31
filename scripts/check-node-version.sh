#!/usr/bin/env bash

set -euo pipefail

node <<'NODE'
const [major, minor] = process.versions.node.split('.').map(Number)
const supported = (major === 20 && minor >= 19) || (major === 22 && minor >= 12) || major > 22

if (!supported) {
  console.error(`Node.js ${process.versions.node} detected. This project requires Node.js 20.19+ or 22.12+.`)
  process.exit(1)
}
NODE

