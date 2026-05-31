---
title: Frontend Development Standards
description: "VocaTa 前端（Vue 3 + TypeScript）开发规范与架构约定。"
inclusion: always
---

# VocaTa 前端开发规范

> 适用于 `vocata-web`（用户端）与 `vocata-admin`（管理端）。通用 AI 协作规则（决策边界 / Definition of Done / 提交规范）见 `AGENTS.md`；目录结构总览见 `.ai-rules/structure.md`。本文件只权威化「前端技术栈与编码约定」。

## 技术栈（已核实版本）

| 维度 | 选型 | 版本 |
|---|---|---|
| 框架 | Vue 3（`<script setup>` + Composition API） | ^3.5 |
| 构建 | Vite | ^7.0 |
| 语言 | TypeScript | ~5.8 |
| 类型检查 | vue-tsc | ^3.0 |
| 路由 | Vue Router | ^4.5 |
| 状态 | Pinia（setup store 写法） | ^3.0 |
| UI | Element Plus | ^2.11 |
| HTTP | Axios | ^1.12 |
| Token 存储 | js-cookie | ^3.0 |
| 单元测试 | Vitest + @vue/test-utils（**仅 vocata-web**） | ^3.2 |
| 样式 | web: Sass/SCSS；admin: Tailwind CSS 4 + postcss-pxtorem | — |
| Lint/格式 | ESLint 9（flat config）+ Prettier 3 | — |

**Node 版本**：`^20.19.0 || >=22.12.0`（见 `package.json` engines 与 `scripts/check-node-version.sh`）。

**关键事实（避免按过时假设写代码）**：
- 工程是 **TypeScript**，入口 `src/main.ts`，配置文件为 `.ts`（`vite.config.ts`、`vitest.config.ts`、`eslint.config.ts`）。**不存在** `main.js` / `.eslintrc.js` / `.prettierrc`。
- ESLint 用 **flat config**（`eslint.config.ts` + `@vue/eslint-config-typescript` + `@vue/eslint-config-prettier`），不是旧的 `extends: @vue/standard`。
- **无 E2E 框架**（无 Cypress / Playwright）。测试 = Vitest 单测，且仅用户端有。
- 用户端用 SCSS（BEM）；管理端用 Tailwind 4（原子化 + rem 适配）。两端样式体系不同：**不要在 web 引入 Tailwind，也不要给 admin 强加 BEM**。
- Token 用 `js-cookie` 存取，不是直接 localStorage。

## 工程结构

```
vocata-web/
├── index.html
├── vite.config.ts  vitest.config.ts  eslint.config.ts
├── tsconfig.json  tsconfig.app.json  tsconfig.node.json  env.d.ts
└── src/
    ├── main.ts  App.vue
    ├── api/          # Axios 实例 + 按业务模块的接口封装
    ├── assets/       # SCSS 样式、图片
    ├── components/   # 可复用组件
    ├── composables/  # 组合式函数（use 开头）
    ├── layouts/      # 布局组件
    ├── router/       # 路由 + 守卫
    ├── store/        # Pinia（index.ts + modules/）
    ├── types/        # TS 类型定义
    ├── utils/        # 工具函数
    ├── views/        # 页面组件
    └── tests/        # Vitest 单测（如 avatar.spec.ts）
```

管理端（`vocata-admin`）结构类似，但：无 `composables/`、无 `tests/`，样式走 Tailwind（`@tailwindcss/vite`，无独立 SCSS 体系）。

## npm 脚本（已核实）

公共：`dev` / `dev:local` / `dev:test`、`build` / `build:local|test|prod`、`preview`、`type-check`（`vue-tsc`）、`lint`、`lint:fix`、`format`、`format:check`、`check`（用 `run-s` 串联 lint→type-check→[test]→build）。

**仅 vocata-web**：`test` / `test:watch`（Vitest）。管理端暂无前端单测脚本。

提交前用 `npm run check`（或根目录 `./scripts/check.sh` 跑全栈）。

## 组件约定

- 一律 `<script setup lang="ts">` + Composition API。
- 组件文件 PascalCase（`CharacterCard.vue`）。
- `vocata-web` 的 `components/` **按功能域分目录**（真实划分：`chat/` `creator/` `discovery/` `profile/` `shell/`），不是 `common/` + `business/` 的两分法。新组件归入对应功能域，跨域通用的才放公共位置。
- 组件通信：父子用 props / emit（`defineProps` / `defineEmits` 带 TS 类型）；跨级 provide/inject；全局状态用 Pinia。不要引入事件总线。

## 状态管理（Pinia，setup store）

- 入口 `store/index.ts` 创建 `pinia` 实例，模块放 `store/modules/`，用 **setup store 写法**（`defineStore('x', () => { ... })`），不是 Options 写法。
- 鉴权 token **不放 Pinia 持久化**，统一走 `@/utils/token`（基于 `js-cookie` 的 `getToken`/`setToken`/`removeToken`）。

## API 层

- Axios 实例集中在 `api/request.ts`：`baseURL` 取 `import.meta.env.VITE_APP_URL`，统一超时、请求/响应拦截器。
- 请求拦截器自动注入 `getToken()`；响应拦截器统一处理 `ApiResponse` 的 `code`/`message`，401 时 `removeToken` 并跳登录。
- 业务接口按模块拆分到 `api/` 下，集中复用 `request` 实例，不在组件里直接 `new axios`。
- 后端返回的 ID 都是 **String**，前端类型定义按 string 处理，不要解析成 number。

## 样式

- **vocata-web**：SCSS + BEM 命名。Element Plus 定制放 `assets/styles/element-overrides.scss`，全局变量/mixin 放 `assets/styles/`。
- **vocata-admin**：Tailwind CSS 4（`@tailwindcss/vite`），配合 `postcss-pxtorem` 做移动端 rem 适配。优先用原子类，不重复造 SCSS 体系。

## 类型与测试

- 共享类型放 `types/`；组件内联类型就近定义。开启严格类型，提交前 `npm run type-check`（`vue-tsc`）必须通过。
- 单元测试（仅 web）：Vitest + `@vue/test-utils`，放 `src/tests/`，文件名 `*.spec.ts`。纯函数/边界逻辑优先补测（参考 `tests/avatar.spec.ts` 对 fallback 头像 trim + SVG 转义的覆盖）。

