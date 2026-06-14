# Docker 开发环境

环境变量、启动顺序和 provider 选择统一见 `docs/开发环境说明.md`。本文只记录 `docker-compose.yml` 的编排职责和 Docker 约定。

## 编排职责

- `docker-compose.yml` 负责本地开发和联调容器
- `docker-compose.test.yml`、`docker-compose.prod.yml` 先保留为后续测试/生产预留文件
- 前端容器采用接近发布态的静态构建，不提供热更新

## 启动模式边界

日常开发优先使用宿主机开发模式：

```bash
docker compose up -d postgres redis
```

然后在宿主机启动后端、用户端和管理端。完整步骤见 `docs/开发环境说明.md`。

全量 Docker 模式用于验证容器构建和近似发布态静态前端：

```bash
docker compose up -d --build
```

全量 Docker 模式下，后端容器使用 `.env` 中的 `DOCKER_DB_HOST`、`DOCKER_DB_PORT`、`DOCKER_REDIS_HOST`、`DOCKER_REDIS_PORT` 连接 compose 内部服务。不要把宿主机连接值 `localhost:55433` / `localhost:6380` 传给容器内后端。

## 服务与 Profile

- `postgres`：开发数据库
- `redis`：开发缓存
- `vocata-server`：后端服务
- `vocata-web`：用户端静态前端
- `vocata-admin`：管理端静态前端
- `pgadmin`：数据库管理工具，使用 `tools` profile
- `mailhog`：邮件调试工具，使用 `tools` profile

## 端口与健康检查

- 宿主机端口通过 `.env` 配置，容器内端口由 compose 固定
- 后端健康检查统一为 `/api/health`
- 前端容器内部统一监听 `8080`
- 前端 API 地址通过构建期变量 `VITE_APP_URL` 注入

## 构建模式

`vocata-web` 和 `vocata-admin` 的 Dockerfile 会执行：

```bash
npm run build:${BUILD_MODE}
```

因此 `.env` 中的 `WEB_BUILD_MODE` 和 `ADMIN_BUILD_MODE` 必须对应实际 npm script。当前支持：

- `development`
- `local`
- `test`
- `prod`
- `production`
