# Database and Frontend Chunk Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare the database migration baseline entry path and reduce frontend production chunk warnings without changing business behavior.

**Architecture:** Keep the database work at the documentation and bootstrap-preparation layer for now: expose a reproducible schema export entry point and keep the migration ADR in `Proposed` until a trusted schema source is available. For the frontend, split only the largest stable vendor groups with Vite `manualChunks` so the app bundles remain functionally identical while reducing the main entry chunk size.

**Tech Stack:** Bash, Markdown, Spring Boot 3.1.4, Java 17, PostgreSQL, Vite 7, Vue 3, TypeScript.

---

### Task 1: Database migration baseline preparation

**Files:**
- Modify: `docs/数据库迁移基线方案.md`
- Modify: `docs/技术债与后续工作.md`
- Modify: `README.md`
- Create: `scripts/export-schema.sh`

- [ ] **Step 1: Add a schema export helper script**

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_DIR"

if [[ ! -f ".env" ]]; then
  echo "Missing .env. Copy .env.example to .env first." >&2
  exit 1
fi

set -a
source .env
set +a

pg_dump \
  --schema-only \
  --no-owner \
  --no-privileges \
  --dbname="postgresql://${DB_USERNAME}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  --file="${1:-docs/database-schema-baseline.sql}"
```

- [ ] **Step 2: Document the export workflow**

```markdown
Run `./scripts/export-schema.sh` against a trusted development or staging database to capture schema-only DDL before any migration PR.
Do not use the output directly as a migration file until it has been reviewed against `.ai-rules/database.md` and the entity mappings.
```

- [ ] **Step 3: Record the preparation state in the roadmap docs**

```markdown
The migration baseline is still preparation-only. The next concrete PR must be based on a trusted schema export, not on entity inference alone.
```

- [ ] **Step 4: Validate the docs and script shape**

Run:
```bash
git diff --check
./scripts/validate-docs.sh
bash -n scripts/export-schema.sh
```
Expected: all commands pass.

- [ ] **Step 5: Commit**

```bash
git add docs/数据库迁移基线方案.md docs/技术债与后续工作.md README.md scripts/export-schema.sh
git commit -m "docs: prepare database migration baseline"
```

### Task 2: Frontend vendor chunk split

**Files:**
- Modify: `vocata-web/vite.config.ts`
- Modify: `vocata-admin/vite.config.ts`
- Modify: `docs/技术债与后续工作.md`

- [ ] **Step 1: Add stable vendor chunk grouping to the web app**

```ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue(), vueDevTools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined
            }
            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return 'element-plus'
            }
            if (id.includes('vue-router')) {
              return 'vue-router'
            }
            if (id.includes('pinia')) {
              return 'pinia'
            }
            if (id.includes('vue')) {
              return 'vue'
            }
            return 'vendor'
          },
        },
      },
    },
    server: {
      port: 3000,
      host: '0.0.0.0',
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_APP_URL,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  }
})
```

- [ ] **Step 2: Add the same vendor grouping to the admin app**

```ts
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue(), vueDevTools(), tailwindcss()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined
            }
            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return 'element-plus'
            }
            if (id.includes('vue-router')) {
              return 'vue-router'
            }
            if (id.includes('pinia')) {
              return 'pinia'
            }
            if (id.includes('vue')) {
              return 'vue'
            }
            return 'vendor'
          },
        },
      },
    },
    server: {
      port: 3001,
      host: true,
      proxy: {
        '/api': {
          target: env.VITE_APP_URL,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  }
})
```

- [ ] **Step 3: Mark the remaining size-heavy assets as follow-up only**

```markdown
Fonts and avatars remain as a separate asset-governance item. Keep them out of this PR to avoid mixing chunking changes with visual asset policy changes.
```

- [ ] **Step 4: Validate the frontend builds and asset report**

Run:
```bash
./scripts/validate-web.sh
./scripts/validate-admin.sh
./scripts/report-frontend-assets.sh
```
Expected: builds pass; the main JS chunk warning should be reduced, even if font assets still dominate total size.

- [ ] **Step 5: Commit**

```bash
git add vocata-web/vite.config.ts vocata-admin/vite.config.ts docs/技术债与后续工作.md
git commit -m "build: split frontend vendor chunks"
```

## Self-Review

- [x] Spec coverage: both requested subtopics are assigned to explicit tasks.
- [x] Placeholder scan: no TBD/TODO placeholders remain in the plan.
- [x] Type consistency: file paths and commands match the current repository layout.
- [x] Scope check: database baseline work is limited to preparation; frontend work is limited to bundling.
