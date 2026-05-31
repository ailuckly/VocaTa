# GitHub Staging Secrets 清单

> 本文件说明 staging 部署所需的 GitHub Secrets 及其来源。**不要在本文档中写入真实 IP、私钥内容或指向本机私钥文件的路径**——这些信息只存在于 GitHub 仓库 Secrets 与维护者本地，不进代码库。

当前 staging 目标机（真实值见 GitHub Secrets，不在此明文记录）：

- `STAGING_HOST=<staging 服务器 IP/域名>`
- `STAGING_USER=deploy`
- SSH 端口固定为 `22`（当前 workflow 已写死）

## 必填 Secrets

staging workflow 只使用仓库级 `Repository secrets`，不依赖 `Environments -> staging`。

只需要 3 个 secrets：

- `STAGING_HOST` — 目标机地址
- `STAGING_USER` — 部署用户（`deploy`）
- `STAGING_SSH_KEY` — 部署私钥（见下）

## STAGING_SSH_KEY 来源

staging deploy key 是一对 ed25519 密钥：

- **私钥**：由维护者在本地生成并保管，**不提交进仓库**。生成后将其完整内容粘贴到 GitHub Secret `STAGING_SSH_KEY`。
- **公钥**：安装到服务器的部署用户 `~/.ssh/authorized_keys`（`deploy`），如需应急可同时装到 `root`。

> 配置步骤：本地 `ssh-keygen -t ed25519` 生成密钥对 → 公钥追加到服务器 `authorized_keys` → 私钥内容填入 GitHub Secret `STAGING_SSH_KEY`。私钥用完即应妥善保管，切勿写入任何仓库内文档或日志。

## 当前服务器登录建议

- 日常部署用户：`deploy`
- 应急用户：`root`
- SSH 密码登录：已禁用
- SSH 公钥登录：已启用

## 服务器配置放哪里

业务配置不放 GitHub Secrets，而是放服务器本地：

- `/home/deploy/deploy/vocata/.env`

兼容旧位置：

- `/home/deploy/deploy/vocata/data/vocata-staging.env`

如果新位置不存在，workflow 会自动从旧位置复制一份。

## 前端 IP 暴露说明

如果服务器本地 `.env` 中 `VITE_APP_URL` 写成绝对地址（如 `http://<staging-host>:9009`），前端构建产物会直接带上这个地址，浏览器里可见。

避免把后端地址打进前端的做法——服务器 `.env` 改成：

- `VITE_APP_URL=/api`

前提：

- 当前前端镜像内 Nginx 已把 `/api` 代理到 `vocata-server:9009`

这样浏览器只请求当前站点的 `/api`，不再把端口和后端地址显式写进前端包里。
